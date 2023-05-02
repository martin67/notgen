package se.terrassorkestern.notgen.service.converter;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.service.StorageService;
import se.terrassorkestern.notgen.service.converter.filters.Binarizer;
import se.terrassorkestern.notgen.service.converter.filters.GreyScaler;
import se.terrassorkestern.notgen.service.converter.filters.Otsu;
import se.terrassorkestern.notgen.service.converter.filters.Standard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;

@Slf4j
public class TryckarrOriginal implements ImageProcessor {

    private final Path path;
    private final Path tmpDir;
    private final Arrangement arrangement;
    private final StorageService storageService;
    private final boolean firstPage;

    public TryckarrOriginal(Path path, Path tmpDir, Arrangement arrangement, StorageService storageService, boolean firstPage) {
        this.path = path;
        this.tmpDir = tmpDir;
        this.arrangement = arrangement;
        this.storageService = storageService;
        this.firstPage = firstPage;
    }

    @Override
    public void run() {
        try {
            BufferedImage image = ImageIO.read(path.toFile());

            String basename = path.getFileName().toString();
            log.debug("Image processing {} ({}x{})", basename, image.getWidth(), image.getHeight());

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
                ImageIO.write(rotated, Files.getFileExtension(path.toString()), path.toFile());
                return;
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
            // Bilderna är inscannade med övre vänstra hörnet mot kanten så kan man beskära och förstora.
            // Standard för sådant som jag scannar
            //
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

            if (log.isTraceEnabled()) {
                ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-1-cropped.png"));
            }

            //
            // Om det är ett omslag så skall det sparas en kopia separat här (innan det skalas om)
            // Spara också en thumbnail i storlek 180 bredd
            // Gör bara detta för default arrangement
            //
            if (firstPage && arrangement.isCover()) {
                log.debug("Saving cover");
                try (OutputStream outputStream = storageService.getCoverOutputStream(arrangement)) {
                    ImageIO.write(image, "jpg", outputStream);
                }

                BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_INT_RGB);
                g = thumbnail.createGraphics();
                g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                g.dispose();
                try (OutputStream outputStream = storageService.getThumbnailOutputStream(arrangement)) {
                    ImageIO.write(thumbnail, "png", outputStream);
                }
                return;
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

            if (log.isTraceEnabled()) {
                ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-2-resized.png"));
            }

            // Setup conversion filters
            GreyScaler greyScaler = new Standard();
            Binarizer binarizer = new Otsu();

            //
            // Change to grey
            //
            image = greyScaler.toGreyScale(image);
            if (log.isTraceEnabled()) {
                ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-3-grey.png"));
            }

            //
            // Change to B/W
            //
            image = binarizer.toBinary(image);
            if (log.isTraceEnabled()) {
                ImageIO.write(image, "png", new File(tmpDir.toFile(), basename + "-4-bw.png"));
            }

            // Write final picture back to original.
            ImageIO.write(image, "png", storageService.replaceExtension(path, ".png").toFile());

            image.flush();
        } catch (Exception e) {
            log.error("Oopsie", e);
        }
    }

}
