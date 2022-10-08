package se.terrassorkestern.notgen.service;

import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.converter.ImageProcessor;
import se.terrassorkestern.notgen.service.converter.PdfAssembler;
import se.terrassorkestern.notgen.service.converter.filters.Binarizer;
import se.terrassorkestern.notgen.service.converter.filters.GreyScaler;
import se.terrassorkestern.notgen.service.converter.filters.Standard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NoteConverterService {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistPackService playlistPackService;
    private final ProgressService progressService;
    private final StorageService storageService;

    @Value("${notgen.google.id.fullscore}")
    private String googleFileIdFullScore;
    @Value("${notgen.google.id.toscore}")
    private String googleFileIdToScore;
    @Value("${notgen.google.id.instrument}")
    private String googleFileIdInstrument;
    @Value("${notgen.google.id.cover}")
    private String googleFileIdCover;
    @Value("${notgen.google.id.original}")
    private String googleFileIdOriginal;
    @Value("${notgen.google.id.thumbnail}")
    private String googleFileIdThumbnail;
    @Value("${notgen.google.id.packs}")
    private String googleFileIdPacks;
    @Value("${notgen.cache.location}")
    private String cacheLocation;
    @Value("${notgen.cache.ttl:10}")
    private int cacheTtl;
    @Value("${notgen.folders.thumbnails}")
    private String thumbnailsFolder;

    public NoteConverterService(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                                PlaylistPackService playlistPackService, ProgressService progressService,
                                StorageService storageService) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistPackService = playlistPackService;
        this.progressService = progressService;
        this.storageService = storageService;
    }

    public void convert(List<Score> scores, boolean upload) throws IOException {
        log.debug("Starting main convert loop");

        NoteConverterStats stats = new NoteConverterStats();
        stats.setStartTime(Instant.now());

        for (Score score : scores) {
            if (score.getScanned()) {
                String msg = "Converting {} ({})" + score.getTitle() + " (id=" + score.getId() + ")";
                log.info(msg);
                progressService.updateProgress(new Progress(100 * stats.getNumberOfSongs() / scores.size(), 0, msg));

                // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
                //score.getScoreParts().sort(Comparator.comparing((ScorePart s) -> s.getInstrument().getSortOrder()));

                Path tmpDir = storageService.getTmpDir(score);
                storageService.downloadScore(score, tmpDir);

                List<Path> extractedFilesList = split(tmpDir, stats, score);
                imageProcess(tmpDir, extractedFilesList, stats, score);
                createInstrumentParts(tmpDir, extractedFilesList, stats, score, upload);
                progressService.updateProgress(new Progress(100, "Done"));
                if (!log.isTraceEnabled()) {
                    FileUtils.deleteDirectory(tmpDir.toFile());
                }
                stats.incrementNumberOfSongs();
                Metrics.counter("notte.songs").increment();
            }
            progressService.updateProgress(new Progress(100, 100, "Completed!"));
        }
        log.info("Finishing main convert loop");
        stats.setEndTime(Instant.now());
        stats.print();
    }

    private void createInstrumentParts(Path tmpDir, List<Path> extractedFilesList, NoteConverterStats stats, Score score, boolean upload) {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }

        try {
            for (ScorePart scorePart : score.getScoreParts()) {
                Path path = Paths.get(tmpDir.toString(), FilenameUtils.getBaseName(score.getFilename()).substring(7)
                        + " - " + scorePart.getInstrument().getName() + ".pdf");
                log.debug("Creating separate score ({}) {}", scorePart.getLength(), path);

                PDDocument doc = new PDDocument();
                PDDocumentInformation pdd = doc.getDocumentInformation();
                pdd.setAuthor(score.getComposer());
                pdd.setTitle(score.getTitle());
                pdd.setSubject(score.getGenre());
                String keywords = scorePart.getInstrument().getName();
                keywords += (score.getArranger() == null) ? "" : ", " + score.getArranger();
                keywords += (score.getYear() == null || score.getYear() == 0) ? "" : ", " + score.getYear().toString();
                pdd.setKeywords(keywords);
                pdd.setCustomMetadataValue("Musik", score.getComposer());
                pdd.setCustomMetadataValue("Text", score.getAuthor());
                pdd.setCustomMetadataValue("Arrangemang", score.getArranger());
                pdd.setCustomMetadataValue("Instrument", scorePart.getInstrument().getName());
                if (score.getYear() != null && score.getYear() > 0) {
                    pdd.setCustomMetadataValue("År", score.getYear().toString());
                } else {
                    pdd.setCustomMetadataValue("År", null);
                }
                pdd.setCustomMetadataValue("Genre", score.getGenre());
                pdd.setCreator("Terrassorkesterns notgenerator 2.0");
                pdd.setModificationDate(Calendar.getInstance());

                for (int i = scorePart.getPage(); i < (scorePart.getPage() + scorePart.getLength()); i++) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    File png = new File(FilenameUtils.removeExtension(extractedFilesList.get(i - 1).toString()) + ".png");
                    File jpg = new File(FilenameUtils.removeExtension(extractedFilesList.get(i - 1).toString()) + ".jpg");
                    File file;
                    if (png.exists()) {
                        file = png;
                    } else {
                        file = jpg;
                    }
                    PDImageXObject pdImage = PDImageXObject.createFromFile(file.toString(), doc);
                    PDPageContentStream contents = new PDPageContentStream(doc, page);
                    PDRectangle mediaBox = page.getMediaBox();
                    contents.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                    contents.beginText();
                    contents.setFont(PDType1Font.HELVETICA_OBLIQUE, 5);
                    contents.setNonStrokingColor(Color.DARK_GRAY);
                    contents.newLineAtOffset(495, 5);
                    contents.showText("Godhetsfullt inscannad av Terrassorkestern");
                    contents.endText();
                    contents.close();
                }
                doc.save(path.toFile());
                doc.close();
                stats.incrementNumberOfPdf();
                storageService.saveScorePart(scorePart, path);

                if (upload) {
                    try {
                        log.debug("Uploading " + path.toString() + " to Google Drive");
                        Map<String, String> map = new HashMap<>();
                        map.put("Title", score.getTitle());
                        map.put("Composer", score.getComposer());
                        map.put("Author", score.getAuthor());
                        map.put("Arranger", score.getArranger());
                        if (score.getYear() != null && score.getYear() > 0) {
                            map.put("Year", score.getYear().toString());
                        } else {
                            map.put("Year", null);
                        }
                        map.put("Genre", score.getGenre());

                        String description;
                        description = score.getTitle() + "\n";
                        if (score.getGenre() != null) {
                            description += score.getGenre() + "\n";
                        }
                        description += "\n";

                        if (score.getComposer() != null && score.getComposer().length() > 0) {
                            description += "Kompositör: " + score.getComposer() + "\n";
                        }
                        if (score.getAuthor() != null && score.getAuthor().length() > 0) {
                            description += "Text: " + score.getAuthor() + "\n";
                        }
                        if (score.getArranger() != null && score.getArranger().length() > 0) {
                            description += "Arrangör: " + score.getArranger() + "\n";
                        }
                        if (score.getYear() != null && score.getYear() > 0) {
                            description += "År: " + score.getYear().toString() + "\n";
                        }

                        description += "\nStämma: " + scorePart.getInstrument().getName();


                        // Stämmor måste ha namn med .pdf så att forScore hittar den
                        String googleId = "";
//                        googleId = googleDriveService.uploadFile(googleFileIdInstrument, "application/pdf", score.getTitle() + ".pdf", path, scorePart.getInstrument().getName(), description, false, map);
                        if (googleId.length() > 0) {
                            scorePart.setGoogleId(googleId);
                            stats.addNumberOfBytes(Files.size(path));
                        }

                        // Sångstämmor skall också sparas som Google Docs (med OCR)
                        if (scorePart.getInstrument().getName().equals("Sång")) {
                            log.debug("Laddar upp sång för OCR");
                            // Det är jpg/png filen som skall laddas upp, inte PDF:en
                            // Klarar bara fallet då sångnoten är en sida (en bild)
                            if (scorePart.getLength() == 1) {

                                File png = new File(FilenameUtils.removeExtension(extractedFilesList.get(scorePart.getPage() - 1).toString()) + ".png");
                                File jpg = new File(FilenameUtils.removeExtension(extractedFilesList.get(scorePart.getPage() - 1).toString()) + ".jpg");
                                File file;
                                String fileType;
                                if (png.exists()) {
                                    file = png;
                                    fileType = "image/png";
                                } else {
                                    file = jpg;
                                    fileType = "image/jpeg";
                                }

//                                googleDriveService.uploadFile(googleFileIdInstrument, fileType, score.getTitle(), file.toPath(), "Sång - OCR", description, true, map);
                                stats.addNumberOfBytes(Files.size(file.toPath()));
                                stats.incrementNumberOfOcr();
                            } else {
                                log.warn("More than one lyrics page for song {}, skipping Google docs OCR upload", score.getId());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void imageProcess(Path tmpDir, List<Path> extractedFilesList, NoteConverterStats stats, Score score) {
        if (!Files.exists(tmpDir) || !score.getImageProcess() || extractedFilesList.isEmpty()) {
            return;
        }

        log.debug("Starting image processing");
        boolean firstPage = true;
        int numberOfImagesProcessed = 0;
        StopWatch oneScoreWatch = new StopWatch("imageProcess " + score.getTitle());
        oneScoreWatch.start();

        for (Path path : extractedFilesList) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                stats.incrementNumberOfImgProcess();
                numberOfImagesProcessed++;

                String basename = FilenameUtils.getName(path.toString());
                log.debug("Image processing {} ({}x{})", FilenameUtils.getName(path.toString()), image.getWidth(), image.getHeight());
                progressService.updateProgress(new Progress(5 + (50 * numberOfImagesProcessed / extractedFilesList.size()),
                        "Image processing file " + numberOfImagesProcessed + " of " + extractedFilesList.size()));
                StopWatch onePageWatch = new StopWatch(score.getTitle() + ", page " + numberOfImagesProcessed);


                //
                // Ta först hand om vissa specialfall i bildbehandlingen
                //

                // Kolla först om man behöver rotera bilden. Vissa är liggande och behöver roteras 90 grader medsols
                if (image.getWidth() > image.getHeight()) {
                    log.debug("Rotating picture");
                    BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = rotated.createGraphics();
                    g2d.rotate(1.5707963268, 0, image.getHeight());
                    g2d.drawImage(image, -image.getHeight(), 0, null);
                    g2d.dispose();
                    // Spara i det format som den filen hade från början
                    ImageIO.write(rotated, FilenameUtils.getExtension(path.toString()), new File(path.toString()));
                    continue;
                }


                // Filer skannade med min skanner är 2550x3501 (300 DPI) eller 1275x1750/1755 (äldre)
                // Orginalen är 170x265 mm vilket motsvarar  2008 x 3130
                // Med lite marginaler för att hantera skillnader i storlek så bir den
                // slutgiltiga bilden 2036x3116 (med 300 DPI). För bilder scannade i 150 DPI så
                // Crop image to 2008x3130

                // För 300 DPI så har jag hittat följande bredder: 2409, 2480, 2550, 2576, 2872 (1)
                int cropWidth;
                int cropHeight;


                //
                // Om bilderna är inscannade med övre vänstra hörnet mot kanten så kan man beskära och förstora.
                // Standard för sådant som jag scannar
                //
                if (score.getUpperleft()) {
                    if (image.getWidth() > 2000) {
                        // 300 DPI
                        cropWidth = 2036;
                        cropHeight = 3116;
                    } else {
                        cropWidth = 1018;
                        cropHeight = 1558;
                    }

                    onePageWatch.start("cropping");
                    BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics g = cropped.getGraphics();
                    g.drawImage(image, 0, 0, cropped.getWidth(), cropped.getHeight(), 0, 0, cropped.getWidth(), cropped.getHeight(), null);
                    g.dispose();
                    image = cropped;
                    onePageWatch.stop();

                    if (log.isTraceEnabled()) {
                        ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-1-cropped.png"));
                    }

                    //
                    // Om det är ett omslag så skall det sparas en kopia separat här (innan det skalas om)
                    // Spara också en thumbnail i storlek 180 bredd
                    //
                    if (firstPage && score.getCover() && score.getColor()) {
                        ImageIO.write(image, "jpg", new File(tmpDir.toFile(), basename + "-cover.jpg"));
                        stats.incrementNumberOfCovers();

                        BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_INT_RGB);
                        g = thumbnail.createGraphics();
                        g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                        g.dispose();
                        ImageIO.write(thumbnail, "png", Paths.get(thumbnailsFolder).resolve(String.format("%04d.png", score.getId())).toFile());
                    }

                    // Resize
                    // Pad on both sides, 2550-2288=262, 262/2=131, => 131-(2288+131)-131
                    onePageWatch.start("resizing");
                    BufferedImage resized = new BufferedImage(2419, 3501, BufferedImage.TYPE_INT_RGB);
                    g = resized.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
                    g.drawImage(image, 149, 0, 2402, 3501, 0, 0, image.getWidth(), image.getHeight(), null);
                    g.dispose();
                    image = resized;
                    onePageWatch.stop();

                    if (log.isTraceEnabled()) {
                        ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-2-resized.png"));
                    }
                }


                if (firstPage && score.getCover() && score.getColor()) {
                    // Don't convert the first page to grey/BW
                    ImageIO.write(image, "jpg", new File(FilenameUtils.removeExtension(path.toString()) + ".jpg"));
                    firstPage = false;
                    continue;
                }

                // Setup conversion filters
                GreyScaler greyScaler = new Standard();
                //Binarizer binarizer = new Otsu();
                Binarizer binarizer = new Standard();


                //
                // Change to grey
                //
                onePageWatch.start("to grey");
                image = greyScaler.toGreyScale(image);
                onePageWatch.stop();
                if (log.isTraceEnabled()) {
                    ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-3-grey.png"));
                }

                //
                // Change to B/W
                //
                onePageWatch.start("to black and white");
                image = binarizer.toBinary(image);
                onePageWatch.stop();
                if (log.isTraceEnabled()) {
                    ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-4-bw.png"));
                }

                // Write final picture back to original
                ImageIO.write(image, "png", new File(FilenameUtils.removeExtension(path.toString()) + ".png"));

                log.debug("Time converting page {}, {} ms", numberOfImagesProcessed, onePageWatch.getTotalTimeMillis());
                log.trace(onePageWatch.prettyPrint());
            } catch (IOException e) {
                e.printStackTrace();
            }

            firstPage = false;
        }
        oneScoreWatch.stop();
        log.info("Total time converting {}, {} ms", score.getTitle(), oneScoreWatch.getTotalTimeMillis());
        //System.out.println(oneScoreWatch.prettyPrint());
    }

    private void imageProcess2(Path tmpDir, List<Path> extractedFilesList, Score score) throws InterruptedException {
        if (!Files.exists(tmpDir) || !score.getImageProcess() || extractedFilesList.isEmpty()) {
            return;
        }

        log.debug("Starting image processing");
        boolean firstPage = true;

        ExecutorService executorService = null;
        try {
            executorService = Executors.newFixedThreadPool(4);
            for (Path path : extractedFilesList) {
                executorService.submit(new ImageProcessor(path, tmpDir, thumbnailsFolder, score, firstPage));
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

    private List<Path> split(Path tmpDir, NoteConverterStats stats, Score score) {

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
        progressService.updateProgress(new Progress(5, "Extracting files"));
        if (FilenameUtils.getExtension(score.getFilename()).equalsIgnoreCase("zip")) {
            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(inFile);

                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());

            } catch (ZipException e) {
                e.printStackTrace();
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
                        if (o instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) o;
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

    public void createInstrumentPacks() {

        // Create a playlist with all songs
        Playlist playlist = new Playlist();
        playlist.setName("Komplett notpack");
        playlist.setDate(LocalDate.now());

        for (Score score : scoreRepository.findByOrderByTitle()) {
            PlaylistEntry playlistEntry = new PlaylistEntry();
            playlistEntry.setText(score.getTitle());
            playlistEntry.setBold(false);
            playlist.getPlaylistEntries().add(playlistEntry);
        }

        // Use the existing playlist function to create a PDF pack
        for (Instrument instrument : instrumentRepository.findAll()) {
            String outputFile;
            String description = "Komplett notpack för " + instrument.getName();

            outputFile = playlistPackService.createPack(playlist, instrument, instrument.getName() + ".pdf");
            Path path = Paths.get(outputFile);

//            if (upload)
//                try {
//                    googleDriveService.uploadFile(googleFileIdPacks, "application/pdf", instrument.getName(),
//                            path, null, description, false, null);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
        }
    }

    public void convert(List<Score> scores) throws IOException, InterruptedException {
        log.debug("Starting main convert loop");
        NoteConverterStats stats = new NoteConverterStats();
        stats.setStartTime(Instant.now());
        StopWatch stopWatch = new StopWatch("convert");
        //stopWatch.start();

        for (Score score : scores) {
            if (score.getScanned()) {
                log.info("Converting: {} ({})", score.getTitle(), score.getId());

                // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
                score.getScoreParts().sort(Comparator.comparing((ScorePart s) -> s.getInstrument().getSortOrder()));

                Path tmpDir = storageService.getTmpDir(score);
                stopWatch.start("download");
                storageService.downloadScore(score, tmpDir);
                stopWatch.stop();

                List<Path> extractedFilesList = split(tmpDir, stats, score);

                stopWatch.start("image process");
                imageProcess2(tmpDir, extractedFilesList, score);
                stopWatch.stop();
                stopWatch.start("pdf creation");
                createPdfs(tmpDir, extractedFilesList, score);
                stopWatch.stop();
            }
        }
        //stopWatch.stop();
        log.debug("Finishing main convert loop, time: {}", stopWatch.prettyPrint());
    }

    public InputStream assemble(List<Score> scores, Setting setting, boolean sortByInstrument) throws IOException, InterruptedException {
        return assemble(scores, new ArrayList<>(setting.getInstruments()), sortByInstrument);
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
        //instruments.sort(Comparator.comparing(Instrument::getSortOrder));

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

        // Create PDF
        if (sortByInstrument) {
            for (Instrument instrument : instruments) {
                log.info("Adding instrument: {} to pdf output", instrument.getName());
                for (Score score : scores) {
                    if (score.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.toPath(score, instrument).toFile());
                    }
                }
            }

        } else {
            for (Score score : scores) {
                log.info("Adding score: {} ({}) to pdf output", score.getTitle(), score.getId());
                for (Instrument instrument : instruments) {
                    if (score.getInstruments().contains(instrument)) {
                        pdfMergerUtility.addSource(storageService.toPath(score, instrument).toFile());
                    }
                }
            }
        }
        pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

}

