package se.terrassorkestern.notgen2;

import com.google.common.net.UrlEscapers;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


// constructor anropas av springboot,så där kan man inte ange vilka sånger som gäller
// däremot så vet man vid anrop (via webb), vilka sånger och vilka options.

// anrop bör se ut något som:
// noteConverter.convert(songList, option1, option2)
//
// alternativ skulle vara att loopa igenom utanför och anrop i stil med
// noteConverter.convert(song, option1, option2)
// men detta känns inte lika snyggt
//
// inuti noteConverter.convert så har man sedan anropo till olika interna metoder (som typ download)

@Service
public class NoteConverter {
    private final Logger log = LoggerFactory.getLogger(NoteConverter.class);

    private static GoogleDrive googleDrive;
    private static final String GOOGLE_DRIVE_ID_FULLSCORE = "0B-ZpHPz-KfoJQUUxTU5JNWFHbWM";
    private static final String GOOGLE_DRIVE_ID_INSTRUMENT = "0B-ZpHPz-KfoJajZFSXV2dTZzZjQ";
    private static final String GOOGLE_DRIVE_ID_TOSCORE = "0B_STqkG31CToVmdfSGxlZ2M0ZXc";
    private static final String GOOGLE_DRIVE_ID_COVER = "0B_STqkG31CToVTlNOFlIRERZcjg";

    private Path tmpDir;                            // Dir for extracting individual parts
    private ArrayList<Path> extractedFilesList;     // List of extracted files


    NoteConverter(){
        log.info("Constructor!");

        log.info("Google init");
        try {
            googleDrive = new GoogleDrive();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


    void convert(List<Song> songs, boolean upload) {
        log.info("Starting main convert loop");

        for (Song song : songs) {
            log.info("Converting " + song.getId() + ", " + song.getTitle());
            // Sortera så att instrumenten är sorterade i sortorder. Fick inte till det med JPA...
            song.getScoreParts().sort((ScorePart s1, ScorePart s2) -> s1.getInstrument().getSortOrder().compareTo(s2.getInstrument().getSortOrder()));
            //song.getScoreParts().sort(Comparator.comparing(Instrument::getSortOrder));
            this.download(song);
            this.split(song);
            this.imageProcess(song);
            this.createFullScore(song, true, upload);
            this.createFullScore(song, false, upload);
            this.createInstrumentParts(song, upload);
            this.cleanup();
        }
        log.info("Finishing main convert loop");
    }


    private void createFullScore(Song song, boolean TOScore, boolean upload) {
        if (!Files.exists(tmpDir))
            return;

        // Ta bort "0123 - " från det nya filnamnet
        Path path = Paths.get(tmpDir.toString(), FilenameUtils.getBaseName(song.getFilename()).substring(7) + ".pdf");

        if (TOScore)
            log.debug("Creating TO score " + path.toString());
        else
            log.debug("Creating full score " + path.toString());

        try {
            PDDocument doc = new PDDocument();
            PDDocumentInformation pdd = doc.getDocumentInformation();
            pdd.setAuthor("Terrassorkestern");
            pdd.setTitle(song.getTitle());
            if (TOScore) {
                pdd.setSubject("TO sättning");
            } else {
                pdd.setSubject("Full sättning");
            }
            pdd.setCustomMetadataValue("Musik", song.getComposer());
            pdd.setCustomMetadataValue("Text", song.getAuthor());
            pdd.setCustomMetadataValue("Arrangemang", song.getArranger());
            if (song.getYear() != null && song.getYear() > 0)
                pdd.setCustomMetadataValue("År", song.getYear().toString());
            else
                pdd.setCustomMetadataValue("År", null);
            pdd.setCustomMetadataValue("Genre", song.getGenre());
            pdd.setCreator("Terrassorkesterns notgenerator 2.0");
            pdd.setModificationDate(Calendar.getInstance());

            // Ta först hand om framsidan om den finns och är i färg. Endast för fulla arr
            // Alltid första filen om den finns
            if (song.isCover() && song.isColor() && !TOScore) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                File file = new File(FilenameUtils.removeExtension(this.extractedFilesList.get(0).toString()) + ".jpg");
                PDImageXObject pdImage = PDImageXObject.createFromFile(file.toString(), doc);
                PDPageContentStream contents = new PDPageContentStream(doc, page);
                PDRectangle mediaBox = page.getMediaBox();
                contents.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                contents.close();
            }

            for (ScorePart scorePart : song.getScoreParts()) {
                // Om det är TO-sättning så hoppa över instrument som inte är standard
                if (TOScore && !scorePart.getInstrument().isStandard())
                    continue;
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
            //ns.addNumberOfPdf(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (upload) {
            try {
                log.debug("Uploading " + path.toString() + " to Google Drive");
                Map<String, String> map = new HashMap<>();
                map.put("Title", song.getTitle());
                map.put("Composer", song.getComposer());
                map.put("Author", song.getAuthor());
                map.put("Arranger", song.getArranger());
                if (song.getYear() != null && song.getYear() > 0)
                    map.put("Year", song.getYear().toString());
                else
                    map.put("Year", null);
                map.put("Genre", song.getGenre());

                // Beskrivning som visas för google
                StringBuilder description;
                description = new StringBuilder(song.getTitle() + "\n");
                if (song.getGenre() != null)
                    description.append(song.getGenre()).append("\n");
                description.append("\n");

                if (song.getComposer() != null && song.getComposer().length() > 0)
                    description.append("Kompositör: ").append(song.getComposer()).append("\n");
                if (song.getAuthor() != null && song.getAuthor().length() > 0)
                    description.append("Text: ").append(song.getAuthor()).append("\n");
                if (song.getArranger() != null && song.getArranger().length() > 0)
                    description.append("Arrangör: ").append(song.getArranger()).append("\n");
                if (song.getYear() != null && song.getYear() > 0)
                    description.append("År: ").append(song.getYear().toString()).append("\n");

                if (TOScore)
                    description.append("\nTO-sättning:\n");
                else
                    description.append("\nFull sättning:\n");

                for (ScorePart scorePart : song.getScoreParts()) {
                    if (TOScore && !scorePart.getInstrument().isStandard())
                        continue;
                    description.append(scorePart.getInstrument().getName()).append("\n");
                }

                if (TOScore) {
                    googleDrive.uploadFile(GOOGLE_DRIVE_ID_TOSCORE, "application/pdf", song.getTitle(), path, null, description.toString(), false, map);
                    //ns.addNumberOfBytes(Files.size(path));
                } else {
                    googleDrive.uploadFile(GOOGLE_DRIVE_ID_FULLSCORE, "application/pdf", song.getTitle(), path, null, description.toString(), false, map);
                    //ns.addNumberOfBytes(Files.size(path));

                    // Ladda också upp omslaget separat (bara om det är bildbehandlat och beskuret)
                    if (song.isCover() && song.isColor() && song.isUpperleft()) {
                        log.debug("Also uploading cover");
                        Path coverPath = Paths.get(this.extractedFilesList.get(0).toString() + "-cover.jpg");
                        NoteConverter.googleDrive.uploadFile(GOOGLE_DRIVE_ID_COVER, "image/jpeg", song.getTitle(), coverPath, null, null, false, null);
                        //ns.addNumberOfBytes(Files.size(coverPath));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void createInstrumentParts(Song song, boolean upload) {
        if (!Files.exists(tmpDir))
            return;

        try {
            for (ScorePart scorePart : song.getScoreParts()) {
                Path path = Paths.get(tmpDir.toString(), FilenameUtils.getBaseName(song.getFilename()).substring(7) +
                        " - " + scorePart.getInstrument().getName() + ".pdf");
                log.debug("Creating separate score (" + scorePart.getLength() + ") " + path.toString());

                PDDocument doc = new PDDocument();
                PDDocumentInformation pdd = doc.getDocumentInformation();
                pdd.setAuthor(song.getComposer());
                pdd.setTitle(song.getTitle());
                pdd.setSubject(song.getGenre());
                String keywords = scorePart.getInstrument().getName();
                keywords += (song.getArranger() == null) ? "" : ", " + song.getArranger();
                keywords += (song.getYear() == null || song.getYear() == 0) ? "" : ", " + song.getYear().toString();
                pdd.setKeywords(keywords);
                pdd.setCustomMetadataValue("Musik", song.getComposer());
                pdd.setCustomMetadataValue("Text", song.getAuthor());
                pdd.setCustomMetadataValue("Arrangemang", song.getArranger());
                if (song.getYear() != null && song.getYear() > 0)
                    pdd.setCustomMetadataValue("År", song.getYear().toString());
                else
                    pdd.setCustomMetadataValue("År", null);
                pdd.setCustomMetadataValue("Genre", song.getGenre());
                pdd.setCreator("Terrassorkesterns notgenerator 2.0");
                pdd.setModificationDate(Calendar.getInstance());

                for (int i = scorePart.getPage(); i < (scorePart.getPage() + scorePart.getLength()); i++) {
                    PDPage page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    File png = new File(FilenameUtils.removeExtension(this.extractedFilesList.get(i - 1).toString()) + ".png");
                    File jpg = new File(FilenameUtils.removeExtension(this.extractedFilesList.get(i - 1).toString()) + ".jpg");
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
                //ns.addNumberOfPdf(1);

                if (upload) {
                    try {
                        log.debug("Uploading " + path.toString() + " to Google Drive");
                        Map<String, String> map = new HashMap<>();
                        map.put("Title", song.getTitle());
                        map.put("Composer", song.getComposer());
                        map.put("Author", song.getAuthor());
                        map.put("Arranger", song.getArranger());
                        if (song.getYear() != null && song.getYear() > 0)
                            map.put("Year", song.getYear().toString());
                        else
                            map.put("Year", null);
                        map.put("Genre", song.getGenre());

                        String description;
                        description = song.getTitle() + "\n";
                        if (song.getGenre() != null)
                            description += song.getGenre() + "\n";
                        description += "\n";

                        if (song.getComposer() != null && song.getComposer().length() > 0)
                            description += "Kompositör: " + song.getComposer() + "\n";
                        if (song.getAuthor() != null && song.getAuthor().length() > 0)
                            description += "Text: " + song.getAuthor() + "\n";
                        if (song.getArranger() != null && song.getArranger().length() > 0)
                            description += "Arrangör: " + song.getArranger() + "\n";
                        if (song.getYear() != null && song.getYear() > 0)
                            description += "År: " + song.getYear().toString() + "\n";

                        description += "\nStämma: " + scorePart.getInstrument().getName();


                        // Stämmor måste ha namn med .pdf så att forScore hittar den
                        NoteConverter.googleDrive.uploadFile(GOOGLE_DRIVE_ID_INSTRUMENT, "application/pdf", song.getTitle() + ".pdf", path, scorePart.getInstrument().getName(), description, false, map);
                        //ns.addNumberOfBytes(Files.size(path));

                        // Sångstämmor skall också sparas som Google Docs (med OCR)
                        if (scorePart.getInstrument().getName().equals("Sång")) {
                            log.debug("Laddar upp sång för OCR");
                            // Det är jpg/png filen som skall laddas upp, inte PDF:en
                            // Klarar bara fallet då sångnoten är en sida (en bild)
                            if (scorePart.getLength() == 1) {

                                File png = new File(FilenameUtils.removeExtension(this.extractedFilesList.get(scorePart.getPage() - 1).toString()) + ".png");
                                File jpg = new File(FilenameUtils.removeExtension(this.extractedFilesList.get(scorePart.getPage() - 1).toString()) + ".jpg");
                                File file;
                                String fileType;
                                if (png.exists()) {
                                    file = png;
                                    fileType = "image/png";
                                } else {
                                    file = jpg;
                                    fileType = "image/jpeg";
                                }

                                NoteConverter.googleDrive.uploadFile(GOOGLE_DRIVE_ID_INSTRUMENT, fileType, song.getTitle(), file.toPath(), "Sång - OCR", description, true, map);
                                //ns.addNumberOfBytes(Files.size(file.toPath()));
                                //ns.addNumberOfOCRs(1);
                            } else {
                                log.warn("More than one lyrics page for song " + song.getId() + ", skipping Google docs OCR upload");
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


    private void imageProcess(Song song) {
        if (!Files.exists(tmpDir) || !song.isImageProcess())
            return;

        log.debug("Starting image processing");
        boolean firstPage = true;
        for (Path path : this.extractedFilesList) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                //ns.addNumberOfImgProcess(1);

                String basename = FilenameUtils.getName(path.toString());
                log.debug("Image processing " + FilenameUtils.getName(path.toString()) + " (" + image.getWidth() + "x" + image.getHeight() + ")");

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

//                    if (FilenameUtils.getExtension(path.toString()).equals("png")) {
//                        ImageIO.write(rotated, "png", new File(FilenameUtils.removeExtension(path.toString()) + "-rot.png"));
//                    } else {
//                        ImageIO.write(rotated, "jpg", new File(FilenameUtils.removeExtension(path.toString()) + "-rot.jpg"));
//                    }
                    continue;
                }


                // Filer skannade med min skanner är 2550x3501 (300 DPI) eller 1275x1750/1755 (äldre)
                // Orginalen är 170x265 mm vilket motsvarar  2008 x 3130
                // Med lite marginaler för att hantera skillnader i storlek så bir den
                // slutgiltiga bilden 2036x3116 (med 300 DPI). För bilder scannade i 150 DPI så
                // Crop image to 2008x3130

                // För 300 DPI så har jag hittat föjande bredder: 2409, 2480, 2550, 2576, 2872 (1)
                int cropWidth, cropHeight;


                //
                // Om bilderna är inscannade med övre vänstra hörnet mot kanten så kan man beskära och förstora.
                // Standard för sådant som jag scannar
                //
                if (song.isUpperleft()) {
                    if (image.getWidth() > 2000) {
                        // 300 DPI
                        cropWidth = 2036;
                        cropHeight = 3116;
                    } else {
                        cropWidth = 1018;
                        cropHeight = 1558;
                    }
                    BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics g = cropped.getGraphics();
                    g.drawImage(image, 0, 0, cropped.getWidth(), cropped.getHeight(), 0, 0, cropped.getWidth(), cropped.getHeight(), null);
                    g.dispose();
                    image = cropped;
                    if (log.isDebugEnabled()) {
                        ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-cropped.png"));
                    }

                    //
                    // Om det är ett omslag så skall det sparas en kopia separat här (innan det skalas om)
                    //
                    if (firstPage && song.isCover() && song.isColor()) {
                        ImageIO.write(image, "jpg", new File(tmpDir.toFile(), basename + "-cover.jpg"));
                        //ns.addNumberOfCovers(1);
                    }

                    // Resize
                    // Pad on both sides, 2550-2288=262, 262/2=131, => 131-(2288+131)-131
                    BufferedImage resized = new BufferedImage(2419, 3501, BufferedImage.TYPE_INT_RGB);
                    g = resized.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
                    g.drawImage(image, 149, 0, 2402, 3501, 0, 0, image.getWidth(), image.getHeight(), null);
                    g.dispose();
                    image = resized;
                    if (log.isDebugEnabled()) {
                        ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-resized.png"));
                    }
                }


                if (firstPage && song.isCover() && song.isColor()) {
                    // Don't convert the first page to grey/BW
                    ImageIO.write(image, "jpg", new File(FilenameUtils.removeExtension(path.toString()) + ".jpg"));
                    firstPage = false;
                    continue;
                }

                //
                // Change to grey
                //
                BufferedImage grey = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

                OtsuBinarize ob = new OtsuBinarize();
                grey = ob.toGray(image);
                image = grey;

                if (log.isDebugEnabled()) {
                    ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-grey.png"));
                }

                //
                // Change to B/W
                //
                BufferedImage bw = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

                bw = ob.binarize(grey);
                image = bw;

                if (log.isDebugEnabled()) {
                    ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-bw.png"));
                }

                // Write final picture back to original
                ImageIO.write(image, "png", new File(FilenameUtils.removeExtension(path.toString()) + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            firstPage = false;
        }
    }


    private void split(Song song) {
        if (!Files.exists(tmpDir))
            return;

        String inFile = new File(tmpDir.toFile(), song.getFilename()).toString();

        // Unzip files into temp directory
        if (FilenameUtils.getExtension(song.getFilename()).toLowerCase().equals("zip")) {
            log.debug("Extracting " + inFile + " to " + tmpDir.toString());
            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(inFile);

                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());

            } catch (ZipException e) {
                e.printStackTrace();
            }

        } else if (FilenameUtils.getExtension(song.getFilename()).toLowerCase().equals("pdf")) {
            log.debug("Extracting " + inFile + " to " + tmpDir.toString());

            try {
                PDDocument document = PDDocument.load(new File(inFile));
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
        }
        // Store name of all extracted files. Order is important!
        // Exclude source file (zip or pdf)
        try {
            this.extractedFilesList = Files
                    .list(tmpDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(this.extractedFilesList);
            //ns.addNumberOfSrcImg(notePartsList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void download(@NotNull Song song) {
        // if the song is not scanned  then just skip
        if (!song.isScanned())
            return;

        // Create temp directory
        try {
            this.tmpDir = Files.createTempDirectory("notkonv-");
            log.debug("Creating temporary directory: " + tmpDir.toString());
        } catch (IOException e) {
            log.error("Can't create temporary directory");
            //e.printStackTrace();
            return;
        }

        // Download note file to tmpDir
        URL url = null;
        try {
            url = new URL("http://notarkiv.hagelin.nu/" + UrlEscapers.urlPathSegmentEscaper().escape(song.getFilename()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            assert url != null;
            log.debug("Downloading " + url.toString());
            FileUtils.copyURLToFile(url, new File(tmpDir.toFile(), song.getFilename()));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        noteFilePath = Paths.get(tmpDir.toString() + File.separator + fileName);
    }


    private void cleanup() {
        if (!Files.exists(tmpDir))
            return;

        log.debug("Cleaning up");
        this.extractedFilesList.clear();

        // Remove the temp directory if we're not in debug mode
        if (!log.isDebugEnabled()) {
            log.debug("Removing temp directory " + tmpDir.toString());
            try {
                Files.walk(tmpDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
