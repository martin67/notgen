package se.terrassorkestern.notgen.service.converter;

import com.google.common.io.Files;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.model.Score;
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

    private final StorageService storageService;

    public PdfAssembler(StorageService storageService) {
        this.storageService = storageService;
    }

    @Async
    public CompletableFuture<Path> assemble(ArrangementPart arrangementPart, Path tmpDir, List<Path> extractedFilesList) {
        Score score = arrangementPart.getArrangement().getScore();
        Path path = Path.of(tmpDir.toString(), Files.getNameWithoutExtension(score.getTitle()).replaceAll("\\W+", "")
                + " - " + arrangementPart.getInstrument().getName() + ".pdf");
        log.debug("Creating separate score ({}) {}", arrangementPart.getLength(), path);

        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation pdd = doc.getDocumentInformation();
            pdd.setAuthor(score.getComposer());
            pdd.setTitle(score.getTitle());
            pdd.setSubject(score.getGenre());
            String keywords = arrangementPart.getInstrument().getName();
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

            for (int i = arrangementPart.getPage(); i < (arrangementPart.getPage() + arrangementPart.getLength()); i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                // Logic: PDF will be extracted to jpg-files
                //        ZIP will be extracted to jpg-files, but then image processed to png
                // They will have the same basename. So the logic is to take the png first if it exists
                Path image;
                Path pngPath = storageService.replaceExtension(extractedFilesList.get(i - 1), ".png");
                Path jpgPath = storageService.replaceExtension(extractedFilesList.get(i - 1), ".jpg");
                if (pngPath.toFile().exists()) {
                    image = pngPath;
                } else {
                    image = jpgPath;
                }
                PDImageXObject pdImage = PDImageXObject.createFromFile(image.toString(), doc);
                PDPageContentStream contents = new PDPageContentStream(doc, page);
                PDRectangle mediaBox = page.getMediaBox();
                contents.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
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
}