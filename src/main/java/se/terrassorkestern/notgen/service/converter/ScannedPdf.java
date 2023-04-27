package se.terrassorkestern.notgen.service.converter;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.service.StorageService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Path;

@Slf4j
public class ScannedPdf implements ImageProcessor {

    private final Path path;
    private final Arrangement arrangement;
    private final StorageService storageService;
    private final boolean firstPage;

    private enum Rotation {Left, Right}

    public ScannedPdf(Path path, Arrangement arrangement, StorageService storageService, boolean firstPage) {
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

            // Kolla först om man behöver rotera bilden. Vissa är liggande och behöver roteras 90 grader medsols
            switch (arrangement.getScoreType()) {
                case PDF -> {
                    if (image.getWidth() > image.getHeight()) {
                        log.warn("Score type is PDF but landscape mode for arr {}, path {}", arrangement, path);
                    }
                }
                case PDF_L -> {
                    image = rotate(Rotation.Left, image);
                    ImageIO.write(image, Files.getFileExtension(path.toString()), path.toFile());
                }
                case PDF_R -> {
                    image = rotate(Rotation.Right, image);
                    ImageIO.write(image, Files.getFileExtension(path.toString()), path.toFile());
                }
                default -> {
                    log.error("Arr {} is not PDF", arrangement);
                    return;
                }
            }

            if (firstPage && arrangement.getCover()) {
                log.debug("Saving cover");
                try (OutputStream outputStream = storageService.getCoverOutputStream(arrangement)) {
                    ImageIO.write(image, "jpg", outputStream);
                }

                BufferedImage thumbnail = new BufferedImage(180, 275, BufferedImage.TYPE_INT_RGB);
                Graphics g = thumbnail.createGraphics();
                g.drawImage(image, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null);
                g.dispose();
                try (OutputStream outputStream = storageService.getThumbnailOutputStream(arrangement)) {
                    ImageIO.write(thumbnail, "png", outputStream);
                }
            }
        } catch (Exception e) {
            log.error("Oopsie", e);
        }
    }

    private BufferedImage rotate(Rotation rotation, BufferedImage image) {
        log.warn("Rotating picture {} for score {}", rotation, arrangement.getScore());
        BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        if (rotation == Rotation.Left) {
            g2d.rotate(Math.toRadians(90), 0, image.getHeight());
            g2d.drawImage(image, -image.getHeight(), 0, null);
        } else {
            g2d.rotate(Math.toRadians(-90), 0, 0);
            g2d.drawImage(image, -image.getWidth(), 0, null);
        }
        g2d.dispose();
        return rotated;
    }
}
