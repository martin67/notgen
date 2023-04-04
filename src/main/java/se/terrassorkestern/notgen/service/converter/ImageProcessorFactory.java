package se.terrassorkestern.notgen.service.converter;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.service.StorageService;

import java.nio.file.Path;

@Slf4j
public class ImageProcessorFactory {

    private ImageProcessorFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static ImageProcessor create(Path path, Path tmpDir, Arrangement arrangement, StorageService storageService, boolean firstPage) {
        // Todo fix
        Score score = arrangement.getScore();
        if (score.getScoreType() == null) {
            log.warn("scoreType not set for score {}", score);
            if (score.getFilename().endsWith(".zip")) {
                return new TryckarrOriginal(path, tmpDir, arrangement, storageService, firstPage);
            } else if (score.getFilename().endsWith(".pdf")) {
                return new ScannedPdf(path, arrangement, storageService, firstPage);
            } else {
                return null;
            }
        } else {
            return switch (score.getScoreType()) {
                case NotScanned -> null;
                case PDF, PDF_L, PDF_R -> new ScannedPdf(path, arrangement, storageService, firstPage);
                case BW -> new BlackAndWhite(path, arrangement, storageService, firstPage);
                case ScannedTryckArr -> new TryckarrOriginal(path, tmpDir, arrangement, storageService, firstPage);
            };
        }
    }
}
