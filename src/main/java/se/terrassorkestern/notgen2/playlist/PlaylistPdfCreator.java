package se.terrassorkestern.notgen2.playlist;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PlaylistPdfCreator {

    ByteArrayInputStream create(Playlist playlist) {

        PDDocument doc = new PDDocument();
        PDDocumentInformation pdd = doc.getDocumentInformation();
        pdd.setAuthor("Terrassorkestern");

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

// Create a new font object selecting one of the PDF base fonts
        PDFont font = PDType1Font.HELVETICA_BOLD;

        try (PDPageContentStream contents = new PDPageContentStream(doc, page))
        {
            contents.beginText();
            contents.setFont(font, 12);
            contents.newLineAtOffset(100, 500);
            contents.showText("Låtlista: " + playlist.getName());
            contents.endText();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            doc.save(byteArrayOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            doc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
