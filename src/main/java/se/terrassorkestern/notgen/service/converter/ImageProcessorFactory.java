package se.terrassorkestern.notgen.service.converter;

import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.service.StorageService;

import java.nio.file.Path;

public class ImageProcessorFactory {
    public static ImageProcessor create(Path path, Path tmpDir, Score score, StorageService storageService, boolean firstPage) {
        return switch (score.getScoreType()) {
            case NotScanned -> null;
            case PDF -> new ScannedPdf(path, tmpDir, score, storageService, false);
            case ScannedTryckArr -> new TryckarrOriginal(path, tmpDir, score, storageService, firstPage);
        };
    }
}
