package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
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
import javax.validation.constraints.NotNull;
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

    @Value("${notgen.google.id.original}")
    private String googleFileIdOriginal;
    @Value("${notgen.cache.location}")
    private String cacheLocation;
    @Value("${notgen.cache.ttl:100}")
    private int cacheTtl;


    public ImageDataExtractor(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    public void extract(List<Score> scores) throws IOException, ImageReadException {

        for (Score score : scores) {
            if (score.getScanned() && score.getFilename() != null) {
                log.info("Extracting image data for {} ({})", score.getTitle(), score.getId());

                Path tmpDir = download(score);
                List<Path> extractedFilesList = split(tmpDir, score);

                int index = 0;
                for (Path path : extractedFilesList) {

                    File file = new File(String.valueOf(path));
                    ImageInfo imageInfo = getImageInfo(file);
                    ImageMetadata imageMetadata = getMetadata(file);

                    Imagedata imageData;
                    if (score.getImageData() == null) {
                        score.setImageData(new ArrayList<>());
                        imageData = new Imagedata();
                        score.getImageData().add(index, imageData);
                    } else if (index == score.getImageData().size()) {
                        imageData = new Imagedata();
                        score.getImageData().add(imageData);
                    } else {
                        imageData = score.getImageData().get(index);
                    }

                    imageData.setPage(index + 1);
                    imageData.setFileSize(Files.size(path));
                    imageData.setFormat(imageInfo.getFormat().getName());
                    imageData.setWidth(imageInfo.getWidth());
                    imageData.setWidthDpi(imageInfo.getPhysicalWidthDpi());
                    imageData.setHeight(imageInfo.getHeight());
                    imageData.setHeightDpi(imageInfo.getPhysicalHeightDpi());
                    imageData.setColorDepth(imageInfo.getBitsPerPixel());
                    imageData.setColorType(imageInfo.getColorType().toString());

                    index++;
                }
                scoreRepository.save(score);
                FileUtils.deleteDirectory(tmpDir.toFile());
            } else {
                log.info("Skipping image data for {} ({})", score.getTitle(), score.getId());
            }
        }
    }

    private List<Path> split(Path tmpDir, Score score) throws IOException {

        List<Path> extractedFilesList = new ArrayList<>();

        if (!Files.exists(tmpDir)) {
            return extractedFilesList;
        }

        File inFile = new File(tmpDir.toFile(), score.getFilename());
        if (!inFile.exists()) {
            return extractedFilesList;
        }

        // Unzip files into temp directory
        log.debug("Extracting {} to {}", inFile, tmpDir);
        if (com.google.common.io.Files.getFileExtension(score.getFilename()).equalsIgnoreCase("zip")) {
            // Initiate ZipFile object with the path/name of the zip file.
            try (ZipFile zipFile = new ZipFile(inFile)) {
                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());
            }
        } else if (com.google.common.io.Files.getFileExtension(score.getFilename()).equalsIgnoreCase("pdf")) {
            try {
                PDDocument document = PDDocument.load(new File(inFile.toString()));
                PDPageTree list = document.getPages();
                int i = 100;
                for (PDPage page : list) {
                    PDResources pdResources = page.getResources();

                    for (COSName name : pdResources.getXObjectNames()) {
                        PDXObject o = pdResources.getXObject(name);
                        if (o instanceof PDImageXObject image) {
                            String filename = tmpDir + File.separator + "extracted-image-" + i;
                            //ImageIO.write(image.getImage(), "png", new File(filename + ".png"));
                            if (image.getImage().getType() == BufferedImage.TYPE_INT_RGB) {
                                ImageIO.write(image.getImage(), "jpg", new File(filename + ".jpg"));
                            } else {
                                ImageIO.write(image.getImage(), "png", new File(filename + ".png"));
                            }
                            i++;
                        }
                    }
                }
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log.error("Unknown file format for {}", score.getFilename());
        }
        // Store name of all extracted files. Order is important!
        // Exclude source file (zip or pdf)

        try (Stream<Path> paths = Files.list(tmpDir)) {
            extractedFilesList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(extractedFilesList);
        }

        return extractedFilesList;
    }

    private Path download(@NotNull Score score) throws IOException {

        Path tmpDir = Files.createTempDirectory("notkonv-");
        log.debug("Creating temporary directory {}", tmpDir.toString());

        // If the cache is enabled, check if the file is present and not older than x days (TTL)
        if (cacheLocation != null) {
            Path cache = Path.of(cacheLocation);
            FileTime ttl = FileTime.from(Instant.now().minus(Duration.ofDays(cacheTtl)));
            Path cacheFile = cache.resolve(score.getFilename());
            if (Files.exists(cacheFile) && Files.getLastModifiedTime(cacheFile).compareTo(ttl) > 0) {
                log.debug("Copying {} from cache", score.getFilename());
            } else {
                log.debug("Downloading {} to cache", score.getFilename());
                //googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), cache);
            }
            Files.copy(cacheFile, tmpDir.resolve(score.getFilename()));
        } else {
            log.debug("Downloading {} to {}", score.getFilename(), tmpDir);
            //googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), tmpDir);
        }
        return tmpDir;
    }

}
