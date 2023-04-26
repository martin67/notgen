package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Score;
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
public class ColorArr implements ImageProcessor {

    private final Path path;
    private final Path tmpDir;
    private final Arrangement arrangement;
    private final StorageService storageService;
    private final boolean firstPage;

    public ColorArr(Path path, Path tmpDir, Arrangement arrangement, StorageService storageService, boolean firstPage) {
        this.path = path;
        this.tmpDir = tmpDir;
        this.arrangement = arrangement;
        this.storageService = storageService;
        this.firstPage = firstPage;
    }

    @Override
    public void run() {
        try {
            // Todo fix
            Score score = arrangement.getScore();

            BufferedImage image = ImageIO.read(path.toFile());

            String basename = path.getFileName().toString();
            log.debug("Image processing {} ({}x{})", basename, image.getWidth(), image.getHeight());

            //
            // Ta först hand om vissa specialfall i bildbehandlingen
            //

            // Kolla först om man behöver rotera bilden. Vissa är liggande och behöver roteras 90 grader medsols
            if (image.getWidth() > image.getHeight()) {
                log.warn("width > height!");
                return;
            }

            //
            // Om det är ett omslag så skall det sparas en kopia separat här (innan det skalas om)
            // Spara också en thumbnail i storlek 180 bredd
            // Gör bara detta för default arrangement
            //
            if (firstPage && score.getCover() && arrangement.getScore().getDefaultArrangement() == arrangement) {
                log.debug("Saving cover");
                try (OutputStream outputStream = storageService.getCoverOutputStream(score)) {
                    ImageIO.write(image, "jpg", outputStream);
                }

                BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_INT_RGB);
                Graphics g = thumbnail.createGraphics();
                g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                g.dispose();
                try (OutputStream outputStream = storageService.getThumbnailOutputStream(score)) {
                    ImageIO.write(thumbnail, "png", outputStream);
                }
                return;
            }

            // Setup conversion filters
            GreyScaler greyScaler = new Standard();
            Binarizer binarizer = new Otsu();
            //Binarizer binarizer = new Standard();

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
