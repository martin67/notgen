package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class AzureStorage implements BackendStorage {

    private static final String BLOB_RESOURCE_PATTERN = "azure-blob://%s/%s";
    @Value("${notgen.storage.azure.input-container}")
    private String inputContainer;
    @Value("${notgen.storage.azure.output-container}")
    private String outputContainer;
    private final ResourceLoader resourceLoader;


    public AzureStorage(@Qualifier("azureStorageBlobProtocolResolver") ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Path downloadScore(Score score, Path location) throws IOException {
        String fileName = score.getFilename();
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, inputContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        String fileName = scorePart.getPdfName();
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, outputContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        String fileName = getScorePartName(score, instrument);
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, outputContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        return false;
    }

    public void uploadScore(Score score, Path path) throws IOException {
        log.info("Uploading {}, file {}", score.getTitle(), path);
        //String fileName = score.getFilename();
        String fileName = String.format("1-%d.%s", score.getId(), com.google.common.io.Files.getFileExtension(score.getFilename()));
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, inputContainer, fileName));
        try (OutputStream os = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, os);
            log.debug("write data to container={}, fileName={}", inputContainer, fileName);
        }
    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        String fileName = scorePart.getPdfName();
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, outputContainer, fileName));
        try (OutputStream os = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, os);
            log.debug("write data to container={}, fileName={}", outputContainer, fileName);
        }
    }

    @Override
    public void deleteScoreParts(Score score) throws IOException {

    }

    @Override
    public void cleanOutput() throws IOException {

    }

    private String getScorePartName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }
}
