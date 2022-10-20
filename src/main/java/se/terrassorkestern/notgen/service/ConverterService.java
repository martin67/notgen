package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.converter.ImageProcessor;
import se.terrassorkestern.notgen.service.converter.PdfAssembler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConverterService {

    private final ScoreRepository scoreRepository;
    private final StorageService storageService;
    @Value("${notgen.folders.static}")
    private String staticContentDir;

    public ConverterService(ScoreRepository scoreRepository,
                            StorageService storageService) {
        this.scoreRepository = scoreRepository;
        this.storageService = storageService;
    }

    private void imageProcess(Path tmpDir, List<Path> extractedFilesList, Score score) throws InterruptedException {
        if (!Files.exists(tmpDir) || !score.getImageProcess() || extractedFilesList.isEmpty()) {
            return;
        }

        log.debug("Starting image processing");
        boolean firstPage = true;

        ExecutorService executorService = null;
        try {
            executorService = Executors.newFixedThreadPool(4);
            for (Path path : extractedFilesList) {
                executorService.submit(new ImageProcessor(path, tmpDir, staticContentDir, score, firstPage));
                firstPage = false;
            }
        } finally {
            if (executorService != null) executorService.shutdown();
        }

        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
            log.error("problem terminating image processing");
        }
    }

    private void createPdfs(Path tmpDir, List<Path> extractedFilesList, Score score) throws InterruptedException {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }
        ExecutorService executorService = null;
        try {
            executorService = Executors.newCachedThreadPool();
            //executorService = Executors.newFixedThreadPool(4);
            for (ScorePart scorePart : score.getScoreParts()) {
                executorService.submit(new PdfAssembler(scorePart, tmpDir, storageService, extractedFilesList));
            }
        } finally {
            if (executorService != null) executorService.shutdown();
        }

        if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
            log.error("problem terminating pdf creation");
        }

    }

    private List<Path> split(Path tmpDir, NoteConverterStats stats, Score score) throws IOException {

        List<Path> extractedFilesList = new ArrayList<>();

        if (!Files.exists(tmpDir)) {
            return null;
        }

        File inFile = new File(tmpDir.toFile(), score.getFilename());
        if (!inFile.exists()) {
            return null;
        }
        //String inFile = new File(tmpDir.toFile(), song.getFilename()).toString();

        // Unzip files into temp directory
        log.debug("Extracting {} to {}", inFile, tmpDir);
        if (FilenameUtils.getExtension(score.getFilename()).equalsIgnoreCase("zip")) {
            // Initiate ZipFile object with the path/name of the zip file.
            try (ZipFile zipFile = new ZipFile(inFile)) {
                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());
            }

        } else if (FilenameUtils.getExtension(score.getFilename()).equalsIgnoreCase("pdf")) {
            try {
                PDDocument document = PDDocument.load(new File(inFile.toString()));
                PDPageTree list = document.getPages();
                int i = 100;
                for (PDPage page : list) {
                    PDResources pdResources = page.getResources();

                    for (COSName name : pdResources.getXObjectNames()) {
                        PDXObject o = pdResources.getXObject(name);
                        if (o instanceof PDImageXObject image) {
                            String filename = tmpDir + File.separator + "extracted-image-" + i;
                            //ImageIO.write(image.getImage(), "png", new File(filename + ".png"));
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log.error("Unknown file format for {}", score.getFilename());
        }
        // Store name of all extracted files. Order is important!
        // Exclude source file (zip or pdf)
        try {
            extractedFilesList = Files
                    .list(tmpDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(extractedFilesList);
            stats.addNumberOfSrcImg(extractedFilesList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return extractedFilesList;
    }

    public void convert(List<Score> scores) throws IOException, InterruptedException {
        log.debug("Starting main convert loop");
        NoteConverterStats stats = new NoteConverterStats();
        stats.setStartTime(Instant.now());
        StopWatch stopWatch = new StopWatch("convert scores");

        for (Score score : scores) {
            if (score.getScanned()) {
                log.info("Converting: {} ({})", score.getTitle(), score.getId());

                // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
                score.getScoreParts().sort(Comparator.comparing((ScorePart s) -> s.getInstrument().getSortOrder()));

                Path tmpDir = storageService.getTmpDir(score);
                stopWatch.start("downloadScorePart, " + score.getTitle());
                storageService.downloadScore(score, tmpDir);
                stopWatch.stop();

                List<Path> extractedFilesList = split(tmpDir, stats, score);

                stopWatch.start("image process, " + score.getTitle());
                imageProcess(tmpDir, extractedFilesList, score);
                stopWatch.stop();
                stopWatch.start("pdf creation, " + score.getTitle());
                createPdfs(tmpDir, extractedFilesList, score);
                stopWatch.stop();
            }
        }
        log.debug("Finishing main convert loop, time: {}", stopWatch.prettyPrint());
    }

    public InputStream assemble(List<Score> scores, Setting setting, boolean sortByInstrument) throws IOException, InterruptedException {
        return assemble(scores, new ArrayList<>(setting.getInstruments()), sortByInstrument);
    }

    public InputStream assemble(Score score, Setting setting) throws IOException, InterruptedException {
        return assemble(List.of(score), new ArrayList<>(setting.getInstruments()), false);
    }

    public InputStream assemble(Score score, Instrument instrument) throws IOException, InterruptedException {
        return assemble(List.of(score), List.of(instrument), false);
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

    public InputStream assemble(List<Score> scores, List<Instrument> instruments, boolean sortByInstrument) throws IOException, InterruptedException {

        // Todo: Can we assume that the input always will be sorted?
        //scores.sort(Comparator.comparing(Score::getTitle));
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

        // Check so that all pdfs have been generated before assembling
        for (Score score : scores) {
            if (!storageService.isScoreGenerated(score)) {
                convert(List.of(score));
            }
        }

        // temp directory for all downloads and assembly
        Path tmpDir = storageService.getTmpDir();

        // Create PDF
        if (sortByInstrument) {
            for (Instrument instrument : sortedInstruments) {
                log.info("Adding instrument: {} to pdf output", instrument.getName());
                for (Score score : scores) {
                    if (score.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.downloadScorePart(score, instrument, tmpDir).toFile());
                    }
                }
            }
        } else {
            for (Score score : scores) {
                log.info("Adding score: {} ({}) to pdf output", score.getTitle(), score.getId());
                for (Instrument instrument : sortedInstruments) {
                    if (score.getInstruments().contains(instrument)) {
                        log.debug("Score: {}, instrument: {}", score.getTitle(), instrument.getName());
                        pdfMergerUtility.addSource(storageService.downloadScorePart(score, instrument, tmpDir).toFile());
                    }
                }
            }
        }
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}

