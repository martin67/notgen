package se.terrassorkestern.notgen.service.converter;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.service.StorageService;
import se.terrassorkestern.notgen.service.converter.filters.Binarizer;
import se.terrassorkestern.notgen.service.converter.filters.GreyScaler;
import se.terrassorkestern.notgen.service.converter.filters.Standard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;

@Slf4j
public class BlackAndWhite implements ImageProcessor {

    private final Path path;
    private final Score score;
    private final StorageService storageService;
    private final boolean firstPage;

    public BlackAndWhite(Path path, Score score, StorageService storageService, boolean firstPage) {
        this.path = path;
        this.score = score;
        this.storageService = storageService;
        this.firstPage = firstPage;
    }

    @Override
    public void run() {
        try {
            BufferedImage image = ImageIO.read(path.toFile());

            String basename = path.getFileName().toString();
            log.debug("Image processing {} ({}x{})", basename, image.getWidth(), image.getHeight());

            if (firstPage && score.getCover()) {
                log.debug("Saving cover");
                try (OutputStream outputStream = storageService.getCoverOutputStream(score)) {
                    ImageIO.write(image, "jpg", outputStream);
                }

                BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_BYTE_BINARY);
                Graphics g = thumbnail.createGraphics();
                g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                g.dispose();
                try (OutputStream outputStream = storageService.getThumbnailOutputStream(score)) {
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
