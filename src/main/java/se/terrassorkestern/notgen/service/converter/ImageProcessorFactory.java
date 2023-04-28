package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.service.StorageService;

import java.nio.file.Path;

@Slf4j
public class ImageProcessorFactory {

    private ImageProcessorFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static ImageProcessor create(Path path, Path tmpDir, Arrangement arrangement, StorageService storageService, boolean firstPage) {
        if (arrangement.getScoreType() == null) {
            log.warn("scoreType not set for score {}, arr {}", arrangement.getScore(), arrangement);
            return null;
        } else {
            return switch (arrangement.getScoreType()) {
                case NOT_SCANNED -> null;
                case PDF, PDF_L, PDF_R -> new ScannedPdf(path, arrangement, storageService, firstPage);
                case BW -> new BlackAndWhite(path, arrangement, storageService, firstPage);
                case COLOR -> new ColorArr(path, tmpDir, arrangement, storageService, firstPage);
                case SCANNED_TRYCK_ARR -> new TryckarrOriginal(path, tmpDir, arrangement, storageService, firstPage);
            };
        }
    }
}
