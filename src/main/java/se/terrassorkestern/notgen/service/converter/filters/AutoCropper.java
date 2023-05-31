package se.terrassorkestern.notgen.service.converter.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.ConfigurationKey;
import se.terrassorkestern.notgen.repository.ConfigurationKeyRepository;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@Service
public class AutoCropper {
    public static final String CONFIG_AUTOCROPPER_TOLERANCE = "AUTOCROPPER_TOLERANCE";
    public static final String CONFIG_AUTOCROPPER_DETECTION_RADIUS = "AUTOCROPPER_DETECTION_RADIUS";

    private final ConfigurationKeyRepository configurationKeyRepository;


    public AutoCropper(ConfigurationKeyRepository configurationKeyRepository) {
        this.configurationKeyRepository = configurationKeyRepository;
        setupConfig();
    }

    public BufferedImage crop(BufferedImage source, int detectionRadius, double tolerance, boolean writeCross) {
        // Get top-left pixel color as the "baseline" for cropping
        //int baseColor = source.getRGB(0, 0);    // upper left
        //int baseColor = Color.WHITE.getRGB();
        int baseColor = new Color(255, 255, 255, 255).getRGB();
//        log.info("White: {}", baseColor);
        int width = source.getWidth();
        int height = source.getHeight();
        //baseColor = (source.getRGB(0,0) + source.getRGB(0, height - 1) + source.getRGB(width - 1, 0) + source.getRGB(width -1, height - 1)) / 4;
        int minX = 0;
        int minY = 0;
        int maxX = width;
        int maxY = height;

        //log.info("baseColor: {}", baseColor);

        // Immediately break the loops when encountering a non-white pixel.
        // top
        label1:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (colorWithinArea(source, detectionRadius, baseColor, x, y, tolerance)) {
                    minY = y;
                    if (writeCross) {
                        cross(source, x, y);
                    }
                    break label1;
                }
            }
        }

        // left
        label2:
        for (int x = 0; x < width; x++) {
            for (int y = minY; y < height; y++) {
                if (colorWithinArea(source, detectionRadius, baseColor, x, y, tolerance)) {
                    minX = x;
                    if (writeCross) {
                        cross(source, x, y);
                    }
                    break label2;
                }
            }
        }

        // Get lower-left pixel color as the "baseline" for cropping
        //baseColor = source.getRGB(minX, height - 1);

        label3:
        for (int y = height - 1; y >= minY; y--) {
            for (int x = minX; x < width; x++) {
                if (colorWithinArea(source, detectionRadius, baseColor, x, y, tolerance)) {
                    maxY = y;
                    if (writeCross) {
                        cross(source, x, y);
                    }
                    break label3;
                }
            }
        }

        label4:
        for (int x = width - 1; x >= minX; x--) {
            for (int y = minY; y < maxY; y++) {
                if (colorWithinArea(source, detectionRadius, baseColor, x, y, tolerance)) {
                    maxX = x;
                    if (writeCross) {
                        cross(source, x, y);
                    }
                    break label4;
                }
            }
        }


        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;

        // if same size, return the original
        if (newWidth == width && newHeight == height) {
            return source;
        }

        BufferedImage target = new BufferedImage(newWidth, newHeight, source.getType());
        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, target.getWidth(), target.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
        g.dispose();

        log.info("**** done");
        return target;
    }

    private boolean colorWithinTolerance(int a, int b, double tolerance) {
        int aRed = (a & 0x00FF0000) >>> 16;   // Red level
        int aGreen = (a & 0x0000FF00) >>> 8;    // Green level
        int aBlue = a & 0x000000FF;            // Blue level

        int bRed = (b & 0x00FF0000) >>> 16;   // Red level
        int bGreen = (b & 0x0000FF00) >>> 8;    // Green level
        int bBlue = b & 0x000000FF;            // Blue level

        double distance = Math.sqrt(
                (aRed - bRed) * (aRed - bRed) +
                        (aGreen - bGreen) * (aGreen - bGreen) +
                        (aBlue - bBlue) * (aBlue - bBlue));

        // 510.0 is the maximum distance between two colors
        // (0,0,0,0 -> 255,255,255,255) => 510.0
        // (0,0,0 -> 255,255,255) => 441.673
        double percentAway = distance / 441.673d;

        return (percentAway > tolerance);
    }

    private boolean colorWithinArea(BufferedImage source, int detectionRadius, int a, int x, int y, double tolerance) {
        for (int dx = Integer.max(0, x - detectionRadius); dx < Integer.min(source.getWidth(), x + detectionRadius); dx++) {
            for (int dy = Integer.max(0, y - detectionRadius); dy < Integer.min(source.getHeight(), y + detectionRadius); dy++) {
                if (!colorWithinTolerance(a, source.getRGB(dx, dy), tolerance)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void cross(BufferedImage source, int x, int y) {
        int size = 20;
        for (int dx = Integer.max(0, x - size); dx < Integer.min(source.getWidth(), x + size); dx++) {
            source.setRGB(dx, y, Color.RED.getRGB());
        }
        for (int dy = Integer.max(0, y - size); dy < Integer.min(source.getHeight(), y + size); dy++) {
            source.setRGB(x, dy, Color.RED.getRGB());
        }
    }

    private void setupConfig() {
        if (configurationKeyRepository.findByToken(CONFIG_AUTOCROPPER_TOLERANCE).isEmpty()) {
            ConfigurationKey key = new ConfigurationKey("AutoCropper", CONFIG_AUTOCROPPER_TOLERANCE,
                    "Tolerance", "Color detection bla", "");
            configurationKeyRepository.save(key);
        }
        if (configurationKeyRepository.findByToken(CONFIG_AUTOCROPPER_DETECTION_RADIUS).isEmpty()) {
            ConfigurationKey key = new ConfigurationKey("AutoCropper", CONFIG_AUTOCROPPER_DETECTION_RADIUS,
                    "Detection radius", "Size of the spot", "");
            configurationKeyRepository.save(key);
        }

    }
}
