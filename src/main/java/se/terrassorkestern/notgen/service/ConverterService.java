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
import org.springframework.util.StopWatch;
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

    private void imageProcess(Path tmpDir, List<Path> extractedFilesList, Arrangement arrangement) throws InterruptedException {
        // TODO temporary fix
        Score score = arrangement.getScore();
        if (!Files.exists(tmpDir) || !score.getImageProcess() || extractedFilesList.isEmpty()) {
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
        if (score.getScanned()) {
            for (Arrangement arrangement : score.getArrangements()) {
                if (arrangement.getArrangementParts().isEmpty()) {
                    log.warn("No parts for score {} ({}), arr {} ({})", score.getTitle(), score.getId(),
                            arrangement.getName(), arrangement.getId());
                } else {
                    log.info("Converting: {} ({}), arr: {} ({})", score.getTitle(), score.getId(),
                            arrangement.getName(), arrangement.getId());

                    // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
                    //score.getScoreParts().sort(Comparator.comparing((ScorePart s) -> s.getInstrument().getSortOrder()));

                    Path tempDir = storageService.createTempDir(score);
                    Path downloadedArrangement = storageService.downloadArrangement(arrangement, tempDir);
                    List<Path> extractedFilesList = split(tempDir, downloadedArrangement);
                    imageProcess(tempDir, extractedFilesList, arrangement);
                    createPdfs(tempDir, extractedFilesList, arrangement);
                    storageService.deleteTempDir(tempDir);
                }
            }
        }
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

        if (!storageService.isScoreGenerated(arrangement.getScore())) {
            convert(List.of(arrangement.getScore()));
        }

        // temp directory for all downloads and assembly
        Path tempDir = storageService.createTempDir();

        log.info("Adding score: {} ({}) to pdf output", arrangement.getScore().getTitle(), arrangement.getScore().getId());
        for (Instrument instrument : instruments) {
            if (arrangement.getInstruments().contains(instrument)) {
                log.debug("Score: {}, arr: {}, instrument: {}", arrangement.getScore().getTitle(), arrangement.getName(), instrument.getName());
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

        // Todo: Can we assume that the input always will be sorted?
        //scores.sort(Comparator.comparing(Score::getTitle));
        StopWatch stopWatch = new StopWatch("Assemble");
        stopWatch.start("setup");
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

        stopWatch.stop();
        stopWatch.start("convert");
        // Check so that all pdfs have been generated before assembling
        for (Score score : scores) {
            if (!storageService.isScoreGenerated(score)) {
                convert(List.of(score));
            }
        }
        stopWatch.stop();
        stopWatch.start("convert");

        // temp directory for all downloads and assembly
        Path tempDir = storageService.createTempDir();

        // Create PDF
        if (sortByInstrument) {
            for (Instrument instrument : sortedInstruments) {
                log.info("Adding instrument: {} to pdf output", instrument.getName());
                for (Score score : scores) {
                    if (score.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.downloadScorePart(score, instrument, tempDir).toFile());
                    }
                }
            }
        } else {
            for (Score score : scores) {
                log.info("Adding score: {} ({}) to pdf output", score.getTitle(), score.getId());
                for (Instrument instrument : sortedInstruments) {
                    if (score.getInstruments().contains(instrument)) {
                        log.debug("Score: {}, instrument: {}", score.getTitle(), instrument.getName());
                        pdfMergerUtility.addSource(storageService.downloadScorePart(score, instrument, tempDir).toFile());
                    }
                }
            }
        }
        stopWatch.stop();
        stopWatch.start("merge");
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        storageService.deleteTempDir(tempDir);

        stopWatch.stop();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}

