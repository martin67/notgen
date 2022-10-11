package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import se.terrassorkestern.notgen.service.StorageService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

@Slf4j
public class PdfAssembler implements Runnable {

    private final ScorePart scorePart;
    private final Path tmpDir;
    private final StorageService storageService;
    private final List<Path> extractedFilesList;

    public PdfAssembler(ScorePart scorePart, Path tmpDir, StorageService storageService, List<Path> extractedFilesList) {
        this.scorePart = scorePart;
        this.tmpDir = tmpDir;
        this.storageService = storageService;
        this.extractedFilesList = extractedFilesList;
    }

    @Override
    public void run() {
        Score score = scorePart.getScore();
        Path path = Path.of(tmpDir.toString(), FilenameUtils.getBaseName(score.getFilename()).substring(7)
                + " - " + scorePart.getInstrument().getName() + ".pdf");
        log.debug("Creating separate score ({}) {}", scorePart.getLength(), path);

        try (PDDocument doc = new PDDocument()) {
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
            pdd.setCreator("Terrassorkesterns notgenerator 3.0");
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
            log.debug("Saving: {}", path);
            storageService.uploadScorePart(scorePart, path);

        } catch (IOException e) {
            log.error("Ooopsie", e);
            throw new RuntimeException(e);
        }
    }
}