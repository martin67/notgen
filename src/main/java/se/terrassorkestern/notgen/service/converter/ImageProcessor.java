package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.service.StorageService;
import se.terrassorkestern.notgen.service.converter.filters.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ImageProcessor {
    private enum Rotation {LEFT, RIGHT}

    private final StorageService storageService;
    private final AutoCropper autoCropper;

    public ImageProcessor(StorageService storageService, AutoCropper autoCropper) {
        this.storageService = storageService;
        this.autoCropper = autoCropper;
    }

    @Async
    public CompletableFuture<Path> process(Path path, Path tmpDir, Arrangement arrangement, boolean firstPage) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        String basename = path.getFileName().toString();
        log.debug("Image processing {} ({}x{})", basename, image.getWidth(), image.getHeight());

        switch (arrangement.getScoreType()) {
            case NOT_SCANNED -> log.warn("Arrangement {} not scanned", arrangement);

            case BW -> {
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                } else {
                    image = resizeToA4(image);
                    saveImage(image, path);
                }
            }

            case COLOR -> {
                // Kolla först om man behöver rotera bilden. Vissa är liggande och behöver roteras 90 grader medsols
                if (image.getWidth() > image.getHeight()) {
                    log.warn("width > height!");
                }
                image = autoCrop(image, tmpDir, basename);
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                } else {
                    image = resizeToA4(image);
                    image = toGrey(image, tmpDir, basename);
                    image = toBW(image, tmpDir, basename);
                    saveImage(image, path);
                }
            }

            case PDF -> {
                if (image.getWidth() > image.getHeight()) {
                    log.warn("Score type is PDF but landscape mode for arr {}, path {}", arrangement, path);
                }
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                }
            }

            case PDF_L -> {
                image = rotate(Rotation.LEFT, image, arrangement);
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                } else {
                    saveImage(image, path);
                }
            }

            case PDF_R -> {
                image = rotate(Rotation.RIGHT, image, arrangement);
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                } else {
                    saveImage(image, path);
                }
            }

            case SCANNED_TRYCK_ARR -> {
                image = cropTryckArr(image, tmpDir, basename);
                if (firstPage && arrangement.isCover()) {
                    saveCover(image, arrangement);
                } else {
                    image = resizeToA4(image);
                    image = toGrey(image, tmpDir, basename);
                    image = toBW(image, tmpDir, basename);
                    saveImage(image, path);
                }
            }
        }
        return CompletableFuture.completedFuture(path);
    }

    private void saveCover(BufferedImage image, Arrangement arrangement) throws IOException {
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
    }

    private void saveImage(BufferedImage image, Path path) throws IOException {
        // Write final picture back to original.
        ImageIO.write(image, "png", storageService.replaceExtension(path, ".png").toFile());
        image.flush();
    }

    private BufferedImage resizeToA4(BufferedImage image) {
        BufferedImage resized = new BufferedImage(2480, 3508, BufferedImage.TYPE_INT_RGB);
        Graphics g = resized.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, resized.getWidth(), resized.getHeight());
        g.drawImage(image, 0, 0, 2480, 3508, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return resized;
    }

    private BufferedImage autoCrop(BufferedImage image, Path tmpDir, String basename) throws IOException {
        BufferedImage nextImage = autoCropper.crop(image, 2, 0.30, false);
        if (log.isTraceEnabled()) {
            ImageIO.write(nextImage, "png", new File(tmpDir.toFile(), basename + "-autocrop.png"));
        }
        return nextImage;
    }

    private BufferedImage cropTryckArr(BufferedImage image, Path tmpDir, String basename) throws IOException {
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

        if (log.isTraceEnabled()) {
            ImageIO.write(cropped, "png", new File(tmpDir.toFile(), basename + "-1-cropped.png"));
        }
        return cropped;
    }

    private BufferedImage toGrey(BufferedImage image, Path tmpDir, String basename) throws IOException {
        GreyScaler greyScaler = new Standard();
        BufferedImage nextImage = greyScaler.toGreyScale(image);
        if (log.isTraceEnabled()) {
            ImageIO.write(nextImage, "png", new File(tmpDir.toFile(), basename + "-3-grey.png"));
        }
        return nextImage;
    }

    private BufferedImage toBW(BufferedImage image, Path tmpDir, String basename) throws IOException {
        Binarizer binarizer = new Otsu();
        BufferedImage nextImage = binarizer.toBinary(image);
        if (log.isTraceEnabled()) {
            ImageIO.write(nextImage, "png", new File(tmpDir.toFile(), basename + "-4-bw.png"));
        }
        return nextImage;
    }

    private BufferedImage rotate(Rotation rotation, BufferedImage image, Arrangement arrangement) {
        log.info("Rotating picture {} for score {}", rotation, arrangement.getScore());
        BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        if (rotation == Rotation.LEFT) {
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
