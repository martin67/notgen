package se.terrassorkestern.notgen2.service;

import io.micrometer.core.instrument.Metrics;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
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
import se.terrassorkestern.notgen2.model.Instrument;
import se.terrassorkestern.notgen2.model.NoteConverterStats;
import se.terrassorkestern.notgen2.model.Progress;
import se.terrassorkestern.notgen2.repository.InstrumentRepository;
import se.terrassorkestern.notgen2.filters.Binarizer;
import se.terrassorkestern.notgen2.filters.GreyScaler;
import se.terrassorkestern.notgen2.filters.Standard;
import se.terrassorkestern.notgen2.model.Playlist;
import se.terrassorkestern.notgen2.model.PlaylistEntry;
import se.terrassorkestern.notgen2.model.Score;
import se.terrassorkestern.notgen2.model.ScorePart;
import se.terrassorkestern.notgen2.repository.ScoreRepository;

import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoteConverterService {
    static final Logger log = LoggerFactory.getLogger(NoteConverterService.class);

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistPackService playlistPackService;
    private final ProgressService progressService;

    @Value("${notgen2.google.id.fullscore}")
    private String googleFileIdFullScore;
    @Value("${notgen2.google.id.toscore}")
    private String googleFileIdToScore;
    @Value("${notgen2.google.id.instrument}")
    private String googleFileIdInstrument;
    @Value("${notgen2.google.id.cover}")
    private String googleFileIdCover;
    @Value("${notgen2.google.id.original}")
    private String googleFileIdOriginal;
    @Value("${notgen2.google.id.thumbnail}")
    private String googleFileIdThumbnail;
    @Value("${notgen2.google.id.packs}")
    private String googleFileIdPacks;
    @Value("${notgen2.cache.location}")
    private String cacheLocation;
    @Value("${notgen2.cache.ttl:10}")
    private int cacheTtl;


    public NoteConverterService(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                                PlaylistPackService playlistPackService, ProgressService progressService) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistPackService = playlistPackService;
        this.progressService = progressService;
    }

    public void convert(List<Score> scores, boolean upload) throws IOException {
        log.info("Starting main convert loop");

        NoteConverterStats stats = new NoteConverterStats();
        stats.setStartTime(Instant.now());

        for (Score score : scores) {
            if (score.getScanned()) {
                String msg = "Converting " + score.getTitle() + " (id=" + score.getId() + ")";
                log.info(msg);
                progressService.updateProgress(new Progress(100 * stats.getNumberOfSongs() / scores.size(), 0, msg));

                // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
                score.getScoreParts().sort(Comparator.comparing((ScorePart s) -> s.getInstrument().getSortOrder()));

                Path tmpDir = download(score);
                List<Path> extractedFilesList = split(tmpDir, stats, score);
                imageProcess(tmpDir, extractedFilesList, stats, score);
                createFullScore(tmpDir, extractedFilesList, stats, score, true, upload);
                createFullScore(tmpDir, extractedFilesList, stats, score, false, upload);
                createInstrumentParts(tmpDir, extractedFilesList, stats, score, upload);
                // update song with new google id:s
                scoreRepository.save(score);
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


    private void createFullScore(Path tmpDir, List<Path> extractedFilesList, NoteConverterStats stats, Score score, boolean toScore, boolean upload) {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }

        // Ta bort "0123 - " från det nya filnamnet
        Path path = Paths.get(tmpDir.toString(), FilenameUtils.getBaseName(score.getFilename()).substring(7) + ".pdf");
        int progressStart;
        int progressLength;

        if (toScore) {
            log.debug("Creating TO score " + path.toString());
            progressStart = 55;
            progressLength = 15;
            progressService.updateProgress(new Progress(progressStart, "Creating TO score"));
        } else {
            log.debug("Creating full score " + path.toString());
            progressStart = 70;
            progressLength = 30;
            progressService.updateProgress(new Progress(progressStart, "Creating full score"));
        }

        try {
            PDDocument doc = new PDDocument();
            PDDocumentInformation pdd = doc.getDocumentInformation();
            pdd.setAuthor("Terrassorkestern");
            pdd.setTitle(score.getTitle());
            if (toScore) {
                pdd.setSubject("TO sättning");
            } else {
                pdd.setSubject("Full sättning");
            }
            pdd.setCustomMetadataValue("Musik", score.getComposer());
            pdd.setCustomMetadataValue("Text", score.getAuthor());
            pdd.setCustomMetadataValue("Arrangemang", score.getArranger());
            if (score.getYear() != null && score.getYear() > 0) {
                pdd.setCustomMetadataValue("År", score.getYear().toString());
            } else {
                pdd.setCustomMetadataValue("År", null);
            }
            pdd.setCustomMetadataValue("Genre", score.getGenre());
            pdd.setCreator("Terrassorkesterns notgenerator 2.0");
            pdd.setModificationDate(Calendar.getInstance());

            // Ta först hand om framsidan om den finns och är i färg. Endast för fulla arr
            // Alltid första filen om den finns
            if (score.getCover() && score.getColor() && !toScore) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                File file = new File(FilenameUtils.removeExtension(extractedFilesList.get(0).toString()) + ".jpg");
                PDImageXObject pdImage = PDImageXObject.createFromFile(file.toString(), doc);
                PDPageContentStream contents = new PDPageContentStream(doc, page);
                PDRectangle mediaBox = page.getMediaBox();
                contents.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                contents.close();
            }

            for (ScorePart scorePart : score.getScoreParts()) {
                progressService.updateProgress(new Progress(progressStart + (progressLength * score.getScoreParts().indexOf(scorePart) / score.getScoreParts().size())));
                // Om det är TO-sättning så hoppa över instrument som inte är standard
//                if (toScore && !scorePart.getInstrument().isStandard()) {
//                    continue;
//                }

                for (int i = scorePart.getPage(); i < (scorePart.getPage() + scorePart.getLength()); i++) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    // Om det är bildbehandlat så är formatet png, i annat fall jpg
                    // TIF från början => zip med png, bildbehandlas ej
                    // PDF => splittas som png, bildbehnandlas ej
                    // ZIP med mina scanningar, splittas som jpg, bildbahandlas och sparas som png
                    // ZIP från Bengtsson, splittas som jpg, behandlas ej, sparas som jpg
                    // Logik - välj i första hand png, finns inte det ta jpg
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
            }
            doc.save(path.toFile());
            doc.close();
            stats.incrementNumberOfPdf();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

                // Beskrivning som visas för google
                StringBuilder description;
                description = new StringBuilder(score.getTitle() + "\n");
                if (score.getGenre() != null) {
                    description.append(score.getGenre()).append("\n");
                }
                description.append("\n");

                if (score.getComposer() != null && score.getComposer().length() > 0) {
                    description.append("Kompositör: ").append(score.getComposer()).append("\n");
                }
                if (score.getAuthor() != null && score.getAuthor().length() > 0) {
                    description.append("Text: ").append(score.getAuthor()).append("\n");
                }
                if (score.getArranger() != null && score.getArranger().length() > 0) {
                    description.append("Arrangör: ").append(score.getArranger()).append("\n");
                }
                if (score.getYear() != null && score.getYear() > 0) {
                    description.append("År: ").append(score.getYear().toString()).append("\n");
                }

                if (toScore) {
                    description.append("\nTO-sättning:\n");
                } else {
                    description.append("\nFull sättning:\n");
                }

                for (ScorePart scorePart : score.getScoreParts()) {
//                    if (toScore && !scorePart.getInstrument().isStandard()) {
//                        continue;
//                    }
                    description.append(scorePart.getInstrument().getName()).append("\n");
                }


                String googleId = "";
                if (toScore) {
//                    googleId = googleDriveService.uploadFile(googleFileIdToScore, "application/pdf", score.getTitle(),
//                            path, null, description.toString(), false, map);
                    if (googleId.length() > 0) {
                        score.setGoogleIdTo(googleId);
                        stats.addNumberOfBytes(Files.size(path));
                    }
                } else {
//                    googleId = googleDriveService.uploadFile(googleFileIdFullScore, "application/pdf", score.getTitle(),
//                            path, null, description.toString(), false, map);
                    if (googleId.length() > 0) {
                        score.setGoogleIdFull(googleId);
                        stats.addNumberOfBytes(Files.size(path));
                    }

                    // Ladda också upp omslaget separat (bara om det är bildbehandlat och beskuret)
                    if (score.getCover() && score.getColor() && score.getUpperleft()) {
                        log.debug("Also uploading cover");
                        Path coverPath = Paths.get(extractedFilesList.get(0).toString() + "-cover.jpg");
//                        googleId = googleDriveService.uploadFile(googleFileIdCover, "image/jpeg", score.getTitle(),
//                                coverPath, null, null, false, null);
                        if (googleId.length() > 0) {
                            score.setGoogleIdCover(googleId);
                            stats.addNumberOfBytes(Files.size(coverPath));
                        }
                        // Om det finns ett omslag så finns det alltid en thumbnail som också skall laddas upp
                        log.debug("Uploading cover thumbnail");
                        Path thumbnailPath = Paths.get(extractedFilesList.get(0).toString() + "-thumbnail.jpg");
//                        googleId = googleDriveService.uploadFile(googleFileIdThumbnail, "image/jpeg", score.getId() + ".jpg",
//                                thumbnailPath, null, null, false, null);
                        if (googleId.length() > 0) {
                            score.setGoogleIdThumbnail(googleId);
                            stats.addNumberOfBytes(Files.size(coverPath));
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void createInstrumentParts(Path tmpDir, List<Path> extractedFilesList, NoteConverterStats stats, Score score, boolean upload) {
        if (!Files.exists(tmpDir) || extractedFilesList.isEmpty()) {
            return;
        }

        try {
            for (ScorePart scorePart : score.getScoreParts()) {
                Path path = Paths.get(tmpDir.toString(), FilenameUtils.getBaseName(score.getFilename()).substring(7)
                        + " - " + scorePart.getInstrument().getName() + ".pdf");
                log.debug("Creating separate score (" + scorePart.getLength() + ") " + path.toString());

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
                                log.warn("More than one lyrics page for song " + score.getId() + ", skipping Google docs OCR upload");
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
                log.debug("Image processing " + FilenameUtils.getName(path.toString()) + " (" + image.getWidth() + "x" + image.getHeight() + ")");
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

                // För 300 DPI så har jag hittat föjande bredder: 2409, 2480, 2550, 2576, 2872 (1)
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
                    // Spara också en thumnbail i storlek 180 bredd
                    //
                    if (firstPage && score.getCover() && score.getColor()) {
                        ImageIO.write(image, "jpg", new File(tmpDir.toFile(), basename + "-cover.jpg"));
                        stats.incrementNumberOfCovers();

                        BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_INT_RGB);
                        g = thumbnail.createGraphics();
                        g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                        g.dispose();
                        ImageIO.write(thumbnail, "jpg", new File(tmpDir.toFile(), basename + "-thumbnail.jpg"));
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

                log.debug("Time converting page " + numberOfImagesProcessed + ", " + onePageWatch.getTotalTimeMillis() + " ms");
                log.trace(onePageWatch.prettyPrint());
            } catch (IOException e) {
                e.printStackTrace();
            }

            firstPage = false;
        }
        oneScoreWatch.stop();
        log.info("Total time converting " + score.getTitle() + ", " + oneScoreWatch.getTotalTimeMillis() + " ms");
        //System.out.println(oneScoreWatch.prettyPrint());
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
        log.debug("Extracting {} to {}", inFile, tmpDir.toString());
        progressService.updateProgress(new Progress(5, "Extracting files"));
        if (FilenameUtils.getExtension(score.getFilename()).toLowerCase().equals("zip")) {
            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(inFile);

                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());

            } catch (ZipException e) {
                e.printStackTrace();
            }

        } else if (FilenameUtils.getExtension(score.getFilename()).toLowerCase().equals("pdf")) {
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
                            String filename = tmpDir.toString() + File.separator + "extracted-image-" + i;
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

    private Path download(@NotNull Score score) throws IOException {

        Path tmpDir = Files.createTempDirectory("notkonv-");
        log.debug("Creating temporary directory {}", tmpDir.toString());

        // If the cache is enabled, check if the file is present and not older than x days (TTL)
        if (cacheLocation != null) {
            Path cache = Paths.get(cacheLocation);
            FileTime ttl = FileTime.from(Instant.now().minus(Duration.ofDays(cacheTtl)));
            Path cacheFile = cache.resolve(score.getFilename());
            if (Files.exists(cacheFile) && Files.getLastModifiedTime(cacheFile).compareTo(ttl) > 0) {
                progressService.updateProgress(new Progress(3, "Copying archive from cache"));
                log.debug("Copying {} from cache", score.getFilename());
            } else {
                log.debug("Downloading {} to cache", score.getFilename());
                progressService.updateProgress(new Progress(3, "Downloading archive from Google"));
//                googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), cache);
            }
            Files.copy(cacheFile, tmpDir.resolve(score.getFilename()));
        } else {
            log.debug("Downloading {} to {}", score.getFilename(), tmpDir);
            progressService.updateProgress(new Progress(3, "Downloading archive from Google"));
//            googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), tmpDir);
        }
        return tmpDir;
    }

    public void createInstrumentPacks(boolean upload) {

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
}

