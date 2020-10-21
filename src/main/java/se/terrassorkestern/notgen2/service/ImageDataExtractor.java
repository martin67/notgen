package se.terrassorkestern.notgen2.service;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.google.GoogleDriveService;
import se.terrassorkestern.notgen2.model.ImageData;
import se.terrassorkestern.notgen2.model.Score;
import se.terrassorkestern.notgen2.repository.ScoreRepository;

import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.imaging.Imaging.getImageInfo;
import static org.apache.commons.imaging.Imaging.getMetadata;

@Service
public class ImageDataExtractor {
    static final Logger log = LoggerFactory.getLogger(ImageDataExtractor.class);

    private final GoogleDriveService googleDriveService;
    private final ScoreRepository scoreRepository;

    @Value("${notgen2.google.id.original}")
    private String googleFileIdOriginal;
    @Value("${notgen2.cache.location}")
    private String cacheLocation;
    @Value("${notgen2.cache.ttl:100}")
    private int cacheTtl;


    public ImageDataExtractor(GoogleDriveService googleDriveService,
                              ScoreRepository scoreRepository) {
        this.googleDriveService = googleDriveService;
        this.scoreRepository = scoreRepository;
    }

    public void extract(List<Score> scores) throws IOException, ImageReadException {

        for (Score score : scores) {
            if (score.getScanned() && score.getFilename() != null) {
                log.info("Extracting image data for " + score.getTitle() + " (id=" + score.getId() + ")");

                Path tmpDir = download(score);
                List<Path> extractedFilesList = split(tmpDir, score);

                int index = 0;
                for (Path path : extractedFilesList) {

                    File file = new File(String.valueOf(path));
                    ImageInfo imageInfo = getImageInfo(file);
                    ImageMetadata imageMetadata = getMetadata(file);

                    ImageData imageData;
                    if (score.getImageData() == null) {
                        score.setImageData(new ArrayList<>());
                        imageData = new ImageData();
                        score.getImageData().add(index, imageData);
                    } else if (index == score.getImageData().size()) {
                        imageData = new ImageData();
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
                log.info("Skipping image data for " + score.getTitle() + " (id=" + score.getId() + ")");
            }
        }
    }

    private List<Path> split(Path tmpDir, Score score) {

        List<Path> extractedFilesList = new ArrayList<>();

        if (!Files.exists(tmpDir)) {
            return extractedFilesList;
        }

        File inFile = new File(tmpDir.toFile(), score.getFilename());
        if (!inFile.exists()) {
            return extractedFilesList;
        }

        // Unzip files into temp directory
        log.debug("Extracting {} to {}", inFile, tmpDir.toString());
        if (FilenameUtils.getExtension(score.getFilename()).toLowerCase().equals("zip")) {
            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(inFile);

                // Extracts all files to the path specified
                zipFile.extractAll(tmpDir.toString());

            } catch (ZipException e) {
                e.printStackTrace();
            }

        } else if (FilenameUtils.getExtension(score.getFilename()).toLowerCase().equals("pdf")) {
            try {
                PDDocument document = PDDocument.load(new File(inFile.toString()));
                PDPageTree list = document.getPages();
                int i = 100;
                for (PDPage page : list) {
                    PDResources pdResources = page.getResources();

                    for (COSName name : pdResources.getXObjectNames()) {
                        PDXObject o = pdResources.getXObject(name);
                        if (o instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) o;
                            String filename = tmpDir.toString() + File.separator + "extracted-image-" + i;
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
        try {
            extractedFilesList = Files
                    .list(tmpDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> (p.toString().toLowerCase().endsWith(".png") || p.toString().toLowerCase().endsWith(".jpg")))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(extractedFilesList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return extractedFilesList;
    }

    private Path download(@NotNull Score score) throws IOException {

        Path tmpDir = Files.createTempDirectory("notkonv-");
        log.debug("Creating temporary directory {}", tmpDir.toString());

        // If the cache is enabled, check if the file is present and not older than x days (TTL)
        if (cacheLocation != null) {
            Path cache = Paths.get(cacheLocation);
            FileTime ttl = FileTime.from(Instant.now().minus(Duration.ofDays(cacheTtl)));
            Path cacheFile = cache.resolve(score.getFilename());
            if (Files.exists(cacheFile) && Files.getLastModifiedTime(cacheFile).compareTo(ttl) > 0) {
                log.debug("Copying {} from cache", score.getFilename());
            } else {
                log.debug("Downloading {} to cache", score.getFilename());
                googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), cache);
            }
            Files.copy(cacheFile, tmpDir.resolve(score.getFilename()));
        } else {
            log.debug("Downloading {} to {}", score.getFilename(), tmpDir);
            googleDriveService.downloadFile(googleFileIdOriginal, score.getFilename(), tmpDir);
        }
        return tmpDir;
    }

}
