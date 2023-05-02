package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.converter.ImageProcessor;
import se.terrassorkestern.notgen.service.converter.ImageProcessorFactory;
import se.terrassorkestern.notgen.service.converter.PdfAssembler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ConverterService implements ItemProcessor<Score, Score> {

    private final ScoreRepository scoreRepository;
    private final StorageService storageService;

    public ConverterService(ScoreRepository scoreRepository, StorageService storageService) {
        this.scoreRepository = scoreRepository;
        this.storageService = storageService;
    }

    @Override
    public Score process(@NonNull Score item) throws Exception {
        convert(item);
        return null;
    }

    public void convert(List<Score> scores) throws IOException, InterruptedException {
        log.debug("Starting main convert loop for {} scores", scores.size());
        for (Score score : scores) {
            convert(score);
        }
        log.debug("Done converting");
    }

    private void convert(Score score) throws IOException, InterruptedException {
        for (Arrangement arrangement : score.getArrangements()) {
            if (arrangement.getArrangementParts().isEmpty()) {
                log.warn("No parts for score {}, arr {}", score, arrangement);
            } else if (arrangement.getFile() == null) {
                log.warn("No file for score {}, arr {}", score, arrangement);
            } else {
                log.info("Converting: {}, arr: {}", score, arrangement);
                Path tempDir = storageService.createTempDir(score);
                Path downloadedArrangement = storageService.downloadArrangement(arrangement, tempDir);
                List<Path> extractedFilesList = split(tempDir, downloadedArrangement);
                imageProcess(tempDir, extractedFilesList, arrangement);
                createPdfs(tempDir, extractedFilesList, arrangement);
                storageService.deleteTempDir(tempDir);
            }
        }
    }

    /**
     * Creates images in png format from the files in extractedFileList. The generated images will be BW and in A4
     * format. The image processing is handled by different converter classes, depending on the format of the indata.
     * The work is processed in multiple threads.
     *
     * @param tmpDir             Temporary directory for working files.
     * @param extractedFilesList All files that was unzipped/unpacked from the original pdf or zip file
     * @param arrangement        Arrangement that should be converted.
     * @throws InterruptedException Exception if interrupted.
     */
    private void imageProcess(Path tmpDir, List<Path> extractedFilesList, Arrangement arrangement) throws InterruptedException {

        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            log.warn("No files for arrangement {}, skipping image processing", arrangement);
            return;
        }

        log.debug("Starting image processing");
        boolean firstPage = true;

        ExecutorService executorService = null;
        try {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (Path path : extractedFilesList) {
                ImageProcessor imageProcessor = ImageProcessorFactory.create(path, tmpDir, arrangement, storageService, firstPage);
                if (imageProcessor != null) {
                    executorService.submit(imageProcessor);
                }
                firstPage = false;
            }
        } finally {
            if (executorService != null) executorService.shutdown();
        }

        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
            log.error("problem terminating image processing");
        }
        log.debug("Finished with image processing");
    }

    /**
     * Creates PDF:s from processed image files
     *
     * @param tmpDir             Directory where the image files are located
     * @param extractedFilesList List of original images
     * @param arrangement        Arrangement to use
     * @throws InterruptedException Exception if interrupted
     */
    private void createPdfs(Path tmpDir, List<Path> extractedFilesList, Arrangement arrangement) throws InterruptedException {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }
        ExecutorService executorService = null;
        try {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (ArrangementPart arrangementPart : arrangement.getArrangementParts()) {
                executorService.submit(new PdfAssembler(arrangementPart, tmpDir, storageService, extractedFilesList));
            }
        } finally {
            if (executorService != null) executorService.shutdown();
        }

        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
            log.error("problem terminating pdf creation");
        }
    }

    public List<Path> split(Path tmpDir, Path downloadedScore) throws IOException {

        if (tmpDir == null || downloadedScore == null || !Files.exists(tmpDir) || !Files.exists(downloadedScore)) {
            return null;
        }

        // Unzip files into temp directory
        switch (com.google.common.io.Files.getFileExtension(downloadedScore.getFileName().toString().toLowerCase())) {
            case "zip" -> storageService.extractZip(downloadedScore, tmpDir);
            case "pdf" -> {
                PDDocument document = PDDocument.load(downloadedScore.toFile());
                PDPageTree list = document.getPages();
                int i = 100;
                for (PDPage page : list) {
                    PDResources pdResources = page.getResources();

                    for (COSName name : pdResources.getXObjectNames()) {
                        PDXObject o = pdResources.getXObject(name);
                        if (o instanceof PDImageXObject image) {
                            String filename = tmpDir + File.separator + "extracted-image-" + i;
                            if (image.getImage().getType() == BufferedImage.TYPE_INT_RGB) {
                                ImageIO.write(image.getImage(), "jpg", new File(filename + ".jpg"));
                            } else {
                                ImageIO.write(image.getImage(), "png", new File(filename + ".png"));
                            }
                            i++;
                        }
                    }
                }
                document.close();
            }
            default -> log.error("Unknown file format for {}", downloadedScore);
        }

        // Store name of all extracted files. Order is important!
        // Exclude source file (zip or pdf)
        List<Path> extractedFilesList;

        try (Stream<Path> paths = Files.list(tmpDir)) {
            extractedFilesList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(extractedFilesList);
        }

        return extractedFilesList;
    }


    public InputStream assemble(List<Score> scores, Setting setting, boolean sortByInstrument) throws
            IOException, InterruptedException {
        return assemble(scores, new ArrayList<>(setting.getInstruments()), sortByInstrument);
    }

    public InputStream assemble(Score score, Setting setting) throws IOException, InterruptedException {
        return assemble(List.of(score), new ArrayList<>(setting.getInstruments()), false);
    }

    public InputStream assemble(Score score, Instrument instrument) throws IOException, InterruptedException {
        return assemble(List.of(score), List.of(instrument), false);
    }

    // New handling for arrangements
    public InputStream assemble(Arrangement arrangement, Instrument instrument) throws IOException, InterruptedException {
        return assemble(arrangement, List.of(instrument));
    }

    public InputStream assemble(Arrangement arrangement, List<Instrument> instruments) throws IOException, InterruptedException {
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pdfMergerUtility.setDestinationStream(byteArrayOutputStream);

        if (!storageService.isArrangementGenerated(arrangement)) {
            convert(List.of(arrangement.getScore()));
        }

        // temp directory for all downloads and assembly
        Path tempDir = storageService.createTempDir();

        log.info("Adding score: {} to pdf output", arrangement.getScore());
        for (Instrument instrument : instruments) {
            if (arrangement.getInstruments().contains(instrument)) {
                log.debug("Score: {}, arr: {}, instrument: {}", arrangement.getScore(), arrangement, instrument);
                pdfMergerUtility.addSource(storageService.downloadArrangementPart(arrangement, instrument, tempDir).toFile());
            }
        }
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        storageService.deleteTempDir(tempDir);
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    public InputStream assemble(Playlist playlist, Instrument instrument) throws IOException, InterruptedException {
        List<Score> scores = new ArrayList<>();
        List<Instrument> instruments = List.of(instrument);

        for (PlaylistEntry playlistEntry : playlist.getPlaylistEntries()) {
            List<Score> scoresFound = scoreRepository.findByTitle(playlistEntry.getText());
            if (scoresFound != null && !scoresFound.isEmpty()) {
                if (scoresFound.size() > 1) {
                    log.warn("Multiple scores for playlist entry {}", playlistEntry.getText());
                }
                scores.add(scoresFound.get(0));
            }
        }
        return assemble(scores, instruments, false);
    }

    public InputStream assemble(List<Score> scores, List<Instrument> instruments, boolean sortByInstrument) throws
            IOException, InterruptedException {

        List<Instrument> sortedInstruments = instruments.stream().sorted(Comparator.comparing(Instrument::getSortOrder)).toList();

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pdfMergerUtility.setDestinationStream(byteArrayOutputStream);

        if (scores.size() > 1) {
            // Use generic info if there are more than one score. Otherwise, use the already present info.
            PDDocumentInformation docInfo = new PDDocumentInformation();
            docInfo.setTitle("Noter");
            docInfo.setAuthor("Terrassorkestern");
            docInfo.setSubject("Notsamling");
            docInfo.setCreator("Terrassorkesterns notgenerator 3.0");
            docInfo.setModificationDate(Calendar.getInstance());

            StringBuilder keywords = new StringBuilder();
            for (Score score : scores) {
                keywords.append(score.getTitle()).append("\n");
            }
            docInfo.setKeywords(keywords.toString());
            pdfMergerUtility.setDestinationDocumentInformation(docInfo);
        }

        // Check so that all pdfs have been generated before assembling. Even though we are checking that all
        // arrangements are generated, we're only using the default arrangement further on.
        for (Score score : scores) {
            if (!storageService.isScoreGenerated(score)) {
                convert(List.of(score));
            }
        }

        // temp directory for all downloads and assembly
        Path tempDir = storageService.createTempDir();

        // Create PDF
        if (sortByInstrument) {
            for (Instrument instrument : sortedInstruments) {
                log.info("Adding instrument: {} to pdf output", instrument);
                for (Score score : scores) {
                    Arrangement arrangement = score.getDefaultArrangement();
                    if (arrangement.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.downloadArrangementPart(arrangement, instrument, tempDir).toFile());
                    }
                }
            }
        } else {
            for (Score score : scores) {
                log.info("Adding score: {} to pdf output", score);
                Arrangement arrangement = score.getDefaultArrangement();
                if (arrangement != null) {
                    for (Instrument instrument : sortedInstruments) {
                        if (arrangement.getInstruments().contains(instrument)) {
                            log.debug("Score: {}, instrument: {}", score, instrument);
                            pdfMergerUtility.addSource(storageService.downloadArrangementPart(arrangement, instrument, tempDir).toFile());
                        }
                    }
                } else {
                    log.warn("No default arrangement for score {}", score);
                }
            }
        }
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        storageService.deleteTempDir(tempDir);

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}

