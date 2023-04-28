package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.service.StorageService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Path;

@Slf4j
public class BlackAndWhite implements ImageProcessor {

    private final Path path;
    private final Arrangement arrangement;
    private final StorageService storageService;
    private final boolean firstPage;

    public BlackAndWhite(Path path, Arrangement arrangement, StorageService storageService, boolean firstPage) {
        this.path = path;
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

            if (firstPage && arrangement.isCover()) {
                log.debug("Saving cover");
                try (OutputStream outputStream = storageService.getCoverOutputStream(arrangement)) {
                    ImageIO.write(image, "jpg", outputStream);
                }

                BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_BYTE_BINARY);
                Graphics g = thumbnail.createGraphics();
                g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                g.dispose();
                try (OutputStream outputStream = storageService.getThumbnailOutputStream(arrangement)) {
                    ImageIO.write(thumbnail, "png", outputStream);
                }
                return;
            }

            // Write final picture back to original.
            ImageIO.write(image, "png", storageService.replaceExtension(path, ".png").toFile());

            image.flush();
        } catch (Exception e) {
            log.error("Oopsie", e);
        }
    }

}
