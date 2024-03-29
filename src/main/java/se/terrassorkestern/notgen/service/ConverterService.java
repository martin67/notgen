package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.converter.ImageProcessor;
import se.terrassorkestern.notgen.service.converter.PdfAssembler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConverterService implements ItemProcessor<Score, Score> {

    private final ScoreRepository scoreRepository;
    private final StorageService storageService;
    private final ImageProcessor imageProcessor;
    private final PdfAssembler pdfAssembler;

    public ConverterService(ScoreRepository scoreRepository, StorageService storageService, ImageProcessor imageProcessor, PdfAssembler pdfAssembler) {
        this.scoreRepository = scoreRepository;
        this.storageService = storageService;
        this.imageProcessor = imageProcessor;
        this.pdfAssembler = pdfAssembler;
    }

    @Override
    public Score process(@NonNull Score item) throws Exception {
        convert(item);
        return null;
    }

    public void convert(List<Score> scores) throws IOException {
        log.debug("Starting main convert loop for {} scores", scores.size());
        for (var score : scores) {
            convert(score);
        }
        log.debug("Done converting");
    }

    private void convert(Score score) throws IOException {
        for (var arrangement : score.getArrangements()) {
            if (arrangement.getArrangementParts().isEmpty()) {
                log.warn("No parts for score {}, arr {}", score, arrangement);
            } else if (arrangement.getFile() == null) {
                log.warn("No file for score {}, arr {}", score, arrangement);
            } else {
                log.info("Converting: {}, arr: {}", score, arrangement);
                var tempDir = storageService.createTempDir(score);
                var downloadedArrangement = storageService.downloadArrangement(arrangement, tempDir);
                var extractedFilesList = split(tempDir, downloadedArrangement);
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
     */
    private void imageProcess(Path tmpDir, List<Path> extractedFilesList, Arrangement arrangement) throws IOException {

        if (arrangement.getScoreType() == ScoreType.NOT_SCANNED) {
            log.warn("Arrangement {} is not scanned, skipping.", arrangement);
            return;
        }
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            log.warn("No files for arrangement {}, skipping image processing", arrangement);
            return;
        }
        if (arrangement.getScoreType() == null) {
            log.warn("scoreType not set for arrangement {}", arrangement);
            return;
        }

        boolean firstPage = true;
        List<CompletableFuture<Path>> completedParts = new ArrayList<>();
        for (var path : extractedFilesList) {
            completedParts.add(imageProcessor.process(path, tmpDir, arrangement, firstPage));
            firstPage = false;
        }
        // Wait until they are all done
        CompletableFuture.allOf(completedParts.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Creates PDF:s from processed image files
     *
     * @param tmpDir             Directory where the image files are located
     * @param extractedFilesList List of original images
     * @param arrangement        Arrangement to use
     */
    private void createPdfs(Path tmpDir, List<Path> extractedFilesList, Arrangement arrangement) {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }
        List<CompletableFuture<Path>> completedParts = new ArrayList<>();
        for (var arrangementPart : arrangement.getArrangementParts()) {
            // multithreaded
            completedParts.add(pdfAssembler.assemble(arrangementPart, tmpDir, extractedFilesList));
        }
        CompletableFuture.allOf(completedParts.toArray(new CompletableFuture[0])).join();
    }

    public List<Path> split(Path tmpDir, Path downloadedScore) throws IOException {

        if (tmpDir == null || downloadedScore == null || !Files.exists(tmpDir) || !Files.exists(downloadedScore)) {
            return List.of();
        }

        // Unzip files into temp directory
        switch (com.google.common.io.Files.getFileExtension(downloadedScore.getFileName().toString().toLowerCase())) {
            case "zip" -> storageService.extractZip(downloadedScore, tmpDir);
            case "pdf" -> {
                try (var document = Loader.loadPDF(downloadedScore.toFile())) {
                    var list = document.getPages();
                    int i = 100;
                    for (var page : list) {
                        var pdResources = page.getResources();

                        for (var name : pdResources.getXObjectNames()) {
                            var o = pdResources.getXObject(name);
                            if (o instanceof PDImageXObject image) {
                                var filename = tmpDir + File.separator + "extracted-image-" + i;
                                if (image.getImage().getType() == BufferedImage.TYPE_INT_RGB) {
                                    ImageIO.write(image.getImage(), "jpg", new File(filename + ".jpg"));
                                } else {
                                    ImageIO.write(image.getImage(), "png", new File(filename + ".png"));
                                }
                                i++;
                            }
                        }
                    }
                }
            }
            default -> log.error("Unknown file format for {}", downloadedScore);
        }

        // Store name of all extracted files. Order is important!
        // Exclude source file (zip or pdf)
        List<Path> extractedFilesList;

        try (var paths = Files.list(tmpDir)) {
            extractedFilesList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(extractedFilesList);
        }

        return extractedFilesList;
    }

    public InputStream assemble(List<Score> scores, Setting setting, boolean sortByInstrument) throws
            IOException {
        return assemble(scores, new ArrayList<>(setting.getInstruments()), sortByInstrument);
    }

    public InputStream assemble(Score score, Setting setting) throws IOException {
        return assemble(List.of(score), new ArrayList<>(setting.getInstruments()), false);
    }

    public InputStream assemble(Score score, Instrument instrument) throws IOException {
        return assemble(List.of(score), List.of(instrument), false);
    }

    public InputStream assemble(Arrangement arrangement, Instrument instrument) throws IOException {
        return assemble(arrangement, List.of(instrument));
    }

    public InputStream assemble(Arrangement arrangement, List<Instrument> instruments) throws IOException {
        var pdfMergerUtility = new PDFMergerUtility();
        var byteArrayOutputStream = new ByteArrayOutputStream();
        pdfMergerUtility.setDestinationStream(byteArrayOutputStream);

        if (!storageService.isArrangementGenerated(arrangement)) {
            convert(List.of(arrangement.getScore()));
        }

        // temp directory for all downloads and assembly
        var tempDir = storageService.createTempDir();

        log.info("Adding score: {} to pdf output", arrangement.getScore());
        for (var instrument : instruments) {
            if (arrangement.getInstruments().contains(instrument)) {
                log.debug("Score: {}, arr: {}, instrument: {}", arrangement.getScore(), arrangement, instrument);
                pdfMergerUtility.addSource(storageService.downloadArrangementPart(arrangement, instrument, tempDir).toFile());
            }
        }
        pdfMergerUtility.mergeDocuments(null);
        storageService.deleteTempDir(tempDir);
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    public InputStream assemble(Playlist playlist, Instrument instrument) throws IOException {
        List<Score> scores = new ArrayList<>();
        var instruments = List.of(instrument);

        for (var playlistEntry : playlist.getPlaylistEntries()) {
            var scoresFound = scoreRepository.findByTitle(playlistEntry.getText());
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
            IOException {

        var sortedInstruments = instruments.stream().sorted(Comparator.comparing(Instrument::getSortOrder)).toList();

        var pdfMergerUtility = new PDFMergerUtility();
        var byteArrayOutputStream = new ByteArrayOutputStream();
        pdfMergerUtility.setDestinationStream(byteArrayOutputStream);

        if (scores.size() > 1) {
            // Use generic info if there are more than one score. Otherwise, use the already present info.
            var docInfo = new PDDocumentInformation();
            docInfo.setTitle("Noter");
            docInfo.setAuthor("Terrassorkestern");
            docInfo.setSubject("Notsamling");
            docInfo.setCreator("Terrassorkesterns notgenerator 3.0");
            docInfo.setModificationDate(Calendar.getInstance());

            var keywords = new StringBuilder();
            for (var score : scores) {
                keywords.append(score.getTitle()).append("\n");
            }
            docInfo.setKeywords(keywords.toString());
            pdfMergerUtility.setDestinationDocumentInformation(docInfo);
        }

        // Check so that all pdfs have been generated before assembling. Even though we are checking that all
        // arrangements are generated, we're only using the default arrangement further on.
        for (var score : scores) {
            if (!storageService.isScoreGenerated(score)) {
                convert(List.of(score));
            }
        }

        // temp directory for all downloads and assembly
        var tempDir = storageService.createTempDir();

        // Create PDF
        if (sortByInstrument) {
            for (var instrument : sortedInstruments) {
                log.info("Adding instrument: {} to pdf output", instrument);
                for (var score : scores) {
                    var arrangement = score.getDefaultArrangement();
                    if (arrangement.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.downloadArrangementPart(arrangement, instrument, tempDir).toFile());
                    }
                }
            }
        } else {
            for (var score : scores) {
                log.info("Adding score: {} to pdf output", score);
                var arrangement = score.getDefaultArrangement();
                if (arrangement != null) {
                    for (var instrument : sortedInstruments) {
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
        pdfMergerUtility.mergeDocuments(null);
        storageService.deleteTempDir(tempDir);

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}

