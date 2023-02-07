package se.terrassorkestern.notgen.service;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Imagedata;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.imaging.Imaging.getImageInfo;
import static org.apache.commons.imaging.Imaging.getMetadata;

@Slf4j
@Service
public class ImageDataExtractor {
    private final ScoreRepository scoreRepository;
    private final StorageService storageService;
    private final ConverterService converterService;

    public ImageDataExtractor(ScoreRepository scoreRepository, StorageService storageService, ConverterService converterService) {
        this.scoreRepository = scoreRepository;
        this.storageService = storageService;
        this.converterService = converterService;
    }

    public void extract(List<Score> scores) throws IOException, ImageReadException {

        for (Score score : scores) {
            if (score.getScanned() && score.getFilename() != null) {
                log.info("Extracting image data for {} ({})", score.getTitle(), score.getId());

                Path tempDir = storageService.createTempDir();
                Path downloadedScore = storageService.downloadScore(score, tempDir);
                List<Path> extractedFilesList = converterService.split(tempDir, downloadedScore);

                // clear old imagedata
                score.getImageData().clear();

                int index = 0;
                for (Path path : extractedFilesList) {

                    File file = new File(String.valueOf(path));
                    ImageInfo imageInfo = getImageInfo(file);
                    ImageMetadata imageMetadata = getMetadata(file);

                    Imagedata imageData = new Imagedata();
                    imageData.setPage(index + 1);
                    imageData.setFileSize(Files.size(path));
                    imageData.setFormat(imageInfo.getFormat().getName());
                    imageData.setWidth(imageInfo.getWidth());
                    imageData.setWidthDpi(imageInfo.getPhysicalWidthDpi());
                    imageData.setHeight(imageInfo.getHeight());
                    imageData.setHeightDpi(imageInfo.getPhysicalHeightDpi());
                    imageData.setColorDepth(imageInfo.getBitsPerPixel());
                    imageData.setColorType(imageInfo.getColorType().toString());

                    if (imageInfo.getWidth() > imageData.getHeight()) {
                        log.warn("Picture is landscape, score: {}, page {}", score, index);
                    }
                    score.getImageData().add(imageData);
                    index++;
                }
                scoreRepository.save(score);
                storageService.deleteTempDir(tempDir);
            } else {
                log.info("Skipping image data for {} ({})", score.getTitle(), score.getId());
            }
        }
    }

}
