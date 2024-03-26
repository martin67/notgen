package se.terrassorkestern.notgen.service.converter;

import com.google.common.io.Files;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.service.StorageService;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class PdfAssembler {
    private static final float POINTS_PER_INCH = 72;
    /** user space units per millimeter */
    private static final float POINTS_PER_MM = 1 / (10 * 2.54f) * POINTS_PER_INCH;

    private final StorageService storageService;

    public PdfAssembler(StorageService storageService) {
        this.storageService = storageService;
    }

    @Async
    public CompletableFuture<Path> assemble(ArrangementPart arrangementPart, Path tmpDir, List<Path> extractedFilesList) {
        var score = arrangementPart.getArrangement().getScore();
        var path = Path.of(tmpDir.toString(), Files.getNameWithoutExtension(score.getTitle()).replaceAll("\\W+", "")
                + " - " + arrangementPart.getInstrument().getName() + ".pdf");
        log.debug("Creating separate score ({}) {}", arrangementPart.getLength(), path);

        try (var doc = new PDDocument()) {
            var pdd = doc.getDocumentInformation();
            pdd.setAuthor(score.getComposer());
            pdd.setTitle(score.getTitle());
            pdd.setSubject(score.getGenre());
            var keywords = arrangementPart.getInstrument().getName();
            keywords += (score.getArranger() == null) ? "" : ", " + score.getArranger();
            keywords += (score.getYear() == null || score.getYear() == 0) ? "" : ", " + score.getYear();
            pdd.setKeywords(keywords);
            pdd.setCustomMetadataValue("Musik", score.getComposer());
            pdd.setCustomMetadataValue("Text", score.getAuthor());
            pdd.setCustomMetadataValue("Arrangemang", score.getArranger());
            pdd.setCustomMetadataValue("Instrument", arrangementPart.getInstrument().getName());
            if (score.getYear() != null && score.getYear() > 0) {
                pdd.setCustomMetadataValue("År", score.getYear().toString());
            } else {
                pdd.setCustomMetadataValue("År", null);
            }
            pdd.setCustomMetadataValue("Genre", score.getGenre());
            pdd.setCreator("Terrassorkesterns notgenerator 3.0");
            pdd.setModificationDate(Calendar.getInstance());

            int pageIndex = 0;
            for (int i = arrangementPart.getPage(); i < (arrangementPart.getPage() + arrangementPart.getLength()); i++) {
                var page = new PDPage(PDRectangle.A4);
                var margin = 20 * POINTS_PER_MM;
                pageIndex++;
                doc.addPage(page);
                // Logic: PDF will be extracted to jpg-files
                //        ZIP will be extracted to jpg-files, but then image processed to png
                // They will have the same basename. So the logic is to take the png first if it exists
                Path image;
                var pngPath = storageService.replaceExtension(extractedFilesList.get(i - 1), ".png");
                var jpgPath = storageService.replaceExtension(extractedFilesList.get(i - 1), ".jpg");
                if (pngPath.toFile().exists()) {
                    image = pngPath;
                } else {
                    image = jpgPath;
                }
                var pdImage = PDImageXObject.createFromFile(image.toString(), doc);
                var contents = new PDPageContentStream(doc, page);
                var mediaBox = page.getMediaBox();
                float leftMargin;

                if (isRightPage(arrangementPart.getLength(), pageIndex)) {
                    leftMargin = margin;
                } else {
                    leftMargin = 0;
                }
                contents.drawImage(pdImage, leftMargin, 0, mediaBox.getWidth() - margin, mediaBox.getHeight());
                contents.beginText();
                contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 5);
                contents.setNonStrokingColor(Color.DARK_GRAY);
                contents.newLineAtOffset(495, 5);
                contents.showText("Godhetsfullt inscannad av Terrassorkestern");
                contents.endText();
                contents.close();
            }
            doc.save(path.toFile());
            log.debug("Saving: {}", path);
            storageService.uploadArrangementPart(arrangementPart, path);
        } catch (IOException e) {
            log.error("Ooopsie", e);
        }
        return CompletableFuture.completedFuture(path);
    }

    private boolean isRightPage(int numberOfPages, int pageIndex) {
        // Om det är jämt antal sidor i arrangementPart så skall första sidan ha marginalen till höger. Är det
        // udda antal sidor skall första vara till vänster. Därefter alternera
        // Antal sidor per stämma   Aktuell sida
        //   1                         1              => högersida
        //   jämn                      jämn           => högersida
        //   jämn                      udda           => vänstersida
        //   udda                      jämn           => högersida
        //   udda                      udda           => vänstersida
        if (numberOfPages == 1 && pageIndex == 1) {
            return true;
        } else {
            return (pageIndex % 2 == 0);
        }
    }
}