package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Playlist;
import se.terrassorkestern.notgen.model.PlaylistEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PlaylistPdfService {

    public ByteArrayInputStream create(Playlist playlist) throws IOException {

        PDDocument doc = new PDDocument();
        PDDocumentInformation pdd = doc.getDocumentInformation();
        pdd.setAuthor("Terrassorkestern");
        pdd.setTitle(playlist.getName() + " " + playlist.getDate().toString());
        pdd.setSubject("Låtlista");

        // (x,y) 0,0 is at lower left corner, 595,841 at top right (A4)
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        // Create a new font object selecting one of the PDF base fonts
        PDFont titleFont = PDType1Font.HELVETICA_BOLD;
        PDFont commentFont = PDType1Font.HELVETICA_OBLIQUE;
        PDFont songFont = PDType1Font.HELVETICA;
        PDFont songFontBold = PDType1Font.HELVETICA_BOLD;
        PDFont songCommentFont = PDType1Font.HELVETICA;

        try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {

            contents.beginText();
            contents.setFont(titleFont, 16);
            contents.newLineAtOffset(200, 775);
            contents.showText(playlist.getName() + "  " + playlist.getDate().toString());
            contents.endText();

            float ypos = drawMultiLineText(playlist.getComment(), 75, 740, 475,
                    page, contents, commentFont, 10, 15) - 20;

            //float ypos = 750;
            for (PlaylistEntry playlistEntry : playlist.getPlaylistEntries()) {
                contents.beginText();

                if (playlistEntry.getBold()) {
                    ypos -= 10;
                    contents.setFont(songFontBold, 12);
                } else {
                    contents.setFont(songFont, 12);
                }
                contents.newLineAtOffset(75, ypos);
                contents.showText(playlistEntry.getText());
                contents.endText();

                if (playlistEntry.getComment() != null) {
                    contents.beginText();
                    contents.setFont(songCommentFont, 10);
                    contents.newLineAtOffset(400, ypos);
                    contents.showText(playlistEntry.getComment());
                    contents.endText();
                }

                ypos -= 20;
                if (playlistEntry.getBold()) {
                    ypos -= 10;
                }

                // Add page(s) if needed
                // TODO: fix
                if (ypos < 50) {
                    //contents.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    //contents = new PDPageContentStream(doc, page);
                    ypos = 750;
                }
            }
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            doc.save(byteArrayOutputStream);
        } catch (IOException e) {
            log.error("Ooopsie: ", e);
        }
        try {
            doc.close();
        } catch (IOException e) {
            log.error("Ooopsie: ", e);
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }


    /**
     * @param text          The text to write on the page.
     * @param x             The position on the x-axis.
     * @param y             The position on the y-axis.
     * @param allowedWidth  The maximum allowed width of the whole text (e.g. the width of the page - a defined margin).
     * @param page          The page for the text.
     * @param contentStream The content stream to set the text properties and write the text.
     * @param font          The font used to write the text.
     * @param fontSize      The font size used to write the text.
     * @param lineHeight    The line height of the font (typically 1.2 * fontSize or 1.5 * fontSize).
     * @throws IOException File error
     */
    private float drawMultiLineText(String text, int x, int y, int allowedWidth, PDPage page, PDPageContentStream contentStream, PDFont font, int fontSize, int lineHeight) throws IOException {

        List<String> lines = new ArrayList<>();

        StringBuilder myLine = new StringBuilder();

        // get all words from the text
        // keep in mind that words are separated by spaces -> "Lorem ipsum!!!!:)" -> words
        // are "Lorem" and "ipsum!!!!:)"
        String[] words = text.split(" ");
        for (String word : words) {

            if (myLine.length() > 0) {
                myLine.append(" ");
            }

            // test the width of the current line + the current word
            int size = (int) (fontSize * font.getStringWidth(myLine + word) / 1000);
            if (size > allowedWidth) {
                // if the line would be too long with the current word, add the line without the current
                // word
                lines.add(myLine.toString());

                // and start a new line with the current word
                myLine = new StringBuilder(word);
            } else {
                // if the current line + the current word would fit, add the current word to the line
                myLine.append(word);
            }
        }
        // add the rest to lines
        lines.add(myLine.toString());

        for (String line : lines) {
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(line);
            contentStream.endText();

            y -= lineHeight;
        }

        return y;
    }
}
