package se.terrassorkestern.notgen.service.storage;

import com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@Component
public class AzureStorage implements BackendStorage {

    private static final String BLOB_RESOURCE_PATTERN = "azure-blob://%s/%s";
    private final ResourceLoader resourceLoader;
    private final AzureStorageBlobProtocolResolver azureStorageBlobProtocolResolver;
    @Value("${notgen.storage.input}")
    private String scoreContainer;
    @Value("${notgen.storage.output}")
    private String scorePartsContainer;
    @Value("${notgen.storage.static}")
    private String staticContainer;


    public AzureStorage(@Qualifier("azureStorageBlobProtocolResolver") ResourceLoader resourceLoader,
                        AzureStorageBlobProtocolResolver patternResolver
    ) {
        this.resourceLoader = resourceLoader;
        this.azureStorageBlobProtocolResolver = patternResolver;
    }

    @Override
    public Path downloadScore(Score score, Path location) throws IOException {
        // Todo the second digit should be the arrangmenet (in the case of multiple arrangements of the same score)
        String fileName = String.format("%d-%d.*", score.getId(), 1);
        Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
        if (resources.length == 0) {
            log.error("No resource found for pattern {}", fileName);
            return null;
        } else if (resources.length > 1) {
            log.warn("Multiple resources found for pattern {}, using the first", fileName);
        }
        Path destination = location.resolve(Objects.requireNonNull(resources[0].getFilename()));
        Files.copy(resources[0].getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        String fileName = getScorePartName(scorePart);
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        String fileName = getScorePartName(score, instrument);
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        return null;
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        return null;
    }

    @Override
    public boolean isScoreGenerated(Score score) throws IOException {
        String pattern = String.format("%d-*.pdf", score.getId());
        Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, pattern));
        // Just check that there are equal number of files as there should be score parts.
        return (resources.length == score.getScoreParts().size());
    }

    public void uploadScore(Score score, Path path) throws IOException {
        log.info("Uploading {}, file {}", score.getTitle(), path);
        String fileName = String.format("%d-1.%s", score.getId(),
                com.google.common.io.Files.getFileExtension(score.getFilename()));
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
        try (OutputStream os = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, os);
            log.debug("write data to container={}, fileName={}", scoreContainer, fileName);
        }
    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        String fileName = getScorePartName(scorePart);
        upload(fileName, path, scorePartsContainer);
    }

    @Override
    public void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException {

    }

    private void upload(String fileName, Path path, String container) throws IOException {
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, container, fileName));
        try (OutputStream outputStream = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, outputStream);
            log.debug("write data to container={}, fileName={}", container, fileName);
        }
    }

    private void upload(String fileName, InputStream inputStream, String container) throws IOException {
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, container, fileName));
        try (OutputStream outputStream = ((WritableResource) storageBlobResource).getOutputStream()) {
            inputStream.transferTo(outputStream);
            log.debug("write data to container={}, fileName={}", container, fileName);
        }
    }

    @Override
    public OutputStream getCoverOutputStream(Score score) throws IOException {
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, staticContainer, getCoverName(score)));
        return ((WritableResource) storageBlobResource).getOutputStream();
    }

    @Override
    public OutputStream getThumbnailOutputStream(Score score) throws IOException {
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, staticContainer, getThumbnailName(score)));
        return ((WritableResource) storageBlobResource).getOutputStream();
    }

    @Override
    public void deleteScore(Score score) {

    }

    @Override
    public void deleteScoreParts(Score score) {

    }

    @Override
    public void cleanOutput() {

    }

}
