package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Playlist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PlaylistPdfService {

    private final ActiveBand activeBand;

    public PlaylistPdfService(ActiveBand activeBand) {
        this.activeBand = activeBand;
    }

    public ByteArrayInputStream create(Playlist playlist) throws IOException {

        var doc = new PDDocument();
        var pdd = doc.getDocumentInformation();
        pdd.setAuthor(activeBand.getBand().getName());
        pdd.setTitle(playlist.getName() + " " + playlist.getDate().toString());
        pdd.setSubject("Låtlista");

        // (x,y) 0,0 is at lower left corner, 595,841 at top right (A4)
        var page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        // Create a new font object selecting one of the PDF base fonts
        var titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        var commentFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        var songFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        var songFontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        var songCommentFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (var contents = new PDPageContentStream(doc, page)) {

            contents.beginText();
            contents.setFont(titleFont, 16);
            contents.newLineAtOffset(200, 775);
            contents.showText(playlist.getName() + "  " + playlist.getDate().toString());
            contents.endText();

            float ypos;
            if (playlist.getComment() != null) {
                ypos = drawMultiLineText(playlist.getComment(), 75, 740, 475,
                        contents, commentFont, 10, 15) - 20;
            } else {
                ypos = 750;
            }
            for (var playlistEntry : playlist.getPlaylistEntries()) {
                contents.beginText();

                if (playlistEntry.isBold()) {
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
                if (playlistEntry.isBold()) {
                    ypos -= 10;
                }

                // Add page(s) if needed
                // TODO: fix
                if (ypos < 50) {
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    ypos = 750;
                }
            }
        }

        var byteArrayOutputStream = new ByteArrayOutputStream();
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
     * @param contentStream The content stream to set the text properties and write the text.
     * @param font          The font used to write the text.
     * @param fontSize      The font size used to write the text.
     * @param lineHeight    The line height of the font (typically 1.2 * fontSize or 1.5 * fontSize).
     * @throws IOException File error
     */
    private float drawMultiLineText(String text, int x, int y, int allowedWidth, PDPageContentStream contentStream, PDFont font, int fontSize, int lineHeight) throws IOException {

        List<String> lines = new ArrayList<>();

        var myLine = new StringBuilder();

        // get all words from the text
        // keep in mind that words are separated by spaces -> "Lorem ipsum!!!!:)" -> words
        // are "Lorem" and "ipsum!!!!:)"
        String[] words = text.split(" ");
        for (var word : words) {

            if (!myLine.isEmpty()) {
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

        for (var line : lines) {
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
