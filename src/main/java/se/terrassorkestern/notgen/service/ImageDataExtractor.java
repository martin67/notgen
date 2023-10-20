package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Imagedata;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ImagedataRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.apache.commons.imaging.Imaging.getImageInfo;

@Slf4j
@Service
public class ImageDataExtractor {
    private final StorageService storageService;
    private final ConverterService converterService;
    private final ImagedataRepository imagedataRepository;

    public ImageDataExtractor(StorageService storageService, ConverterService converterService, ImagedataRepository imagedataRepository) {
        this.storageService = storageService;
        this.converterService = converterService;
        this.imagedataRepository = imagedataRepository;
    }

    public void extract(List<Score> scores) throws IOException, ImageReadException {

        for (var score : scores) {
            for (var arrangement : score.getArrangements())
                if (arrangement.getFile() != null) {
                    log.info("Extracting image data for {} ({}), arr: {} ({})",
                            score.getTitle(), score.getId(), arrangement.getName(), arrangement.getId());

                    var tempDir = storageService.createTempDir();
                    var downloadedArrangement = storageService.downloadArrangement(arrangement, tempDir);
                    var extractedFilesList = converterService.split(tempDir, downloadedArrangement);

                    int index = 0;
                    for (var path : extractedFilesList) {

                        var file = new File(String.valueOf(path));
                        var imageInfo = getImageInfo(file);

                        var imageData = new Imagedata();
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
                        imagedataRepository.save(imageData);
                        index++;
                    }
                    storageService.deleteTempDir(tempDir);
                } else {
                    log.info("Skipping image data for {} ({})", score.getTitle(), score.getId());
                }
        }
    }

}
