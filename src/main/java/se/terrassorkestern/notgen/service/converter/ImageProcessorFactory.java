package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.service.StorageService;

import java.nio.file.Path;

@Slf4j
public class ImageProcessorFactory {
    public static ImageProcessor create(Path path, Path tmpDir, Score score, StorageService storageService, boolean firstPage) {
        if (score.getScoreType() == null) {
            log.warn("scoreType not set for score {}", score);
            if (score.getFilename().endsWith(".zip")) {
                return new TryckarrOriginal(path, tmpDir, score, storageService, firstPage);
            } else if (score.getFilename().endsWith(".pdf")) {
                return new ScannedPdf(path, tmpDir, score, storageService, false);
            } else {
                return null;
            }
        } else {
            return switch (score.getScoreType()) {
                case NotScanned -> null;
                case PDF -> new ScannedPdf(path, tmpDir, score, storageService, false);
                case ScannedTryckArr -> new TryckarrOriginal(path, tmpDir, score, storageService, firstPage);
            };
        }
    }
}
