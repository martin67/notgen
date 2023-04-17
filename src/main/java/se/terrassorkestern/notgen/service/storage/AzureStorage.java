package se.terrassorkestern.notgen.service.storage;

import com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.exceptions.StorageException;
import se.terrassorkestern.notgen.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public Path downloadScore(Score score, Path location) throws IOException {
        // Todo the second digit should be the arrangement (in the case of multiple arrangements of the same score)
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
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        // Arrangement filename is based on the ngFile (ngFileId.extension, i.e. 23.zip)
        // located in the input container
        String fileName = arrangement.getFile().getFilename();
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        // ArrangementPart filename is based on arrangementId-InstrumentId.pdf
        // located in the scoreparts container
        String fileName = getArrangementPartName(arrangement, instrument);
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public boolean isScoreGenerated(Score score) throws IOException {
        // check that all arrangements are generated
        boolean allGenerated = true;

        for (Arrangement arrangement : score.getArrangements()) {
            String pattern = String.format("%d-%d-*.pdf", score.getId(), arrangement.getId());
            Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, pattern));
            // Just check that there are equal number of files as there should be score parts.
            if (resources.length != arrangement.getArrangementParts().size()) {
                allGenerated = false;
            }
        }
        return allGenerated;
    }

    @Override
    public NgFile uploadFile(MultipartFile file) throws StorageException {
        log.error("Upload ({}) not implemented yet!", file.getOriginalFilename());
        return null;
    }

    @Override
    public void deleteFile(String filename) throws StorageException {
        log.error("Delete ({}) not implemented yet!", filename);
    }

    @Override
    public void uploadArrangement(Arrangement arrangement, Path path) throws IOException {
        log.info("Uploading score: {} ({}), arr: {} ({}), file: {}", arrangement.getScore().getTitle(),
                arrangement.getScore().getId(), arrangement.getName(), arrangement.getId(), path);
        String fileName = arrangement.getFile().getFilename();
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
        try (OutputStream os = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, os);
            log.debug("write data to container={}, fileName={}", scoreContainer, fileName);
        }
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
    public InputStream downloadFile(NgFile file) throws StorageException {
        log.error("Download ({}) not implemented yet!", file.getOriginalFilename());
        return null;
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

    // temporary
    @Override
    public Path renameScore(Score score, String newName) throws IOException {
        Path tempDir = Files.createTempDirectory("ng");
        Path downloadedScore = downloadScore(score, tempDir);
        String extension = com.google.common.io.Files.getFileExtension(downloadedScore.toString());
        String newFilename = newName + "." + extension;
        upload(newFilename, downloadedScore, scoreContainer);
        return Paths.get(newFilename);
    }

    @Override
    public void cleanOutput() {

    }

    @Override
    public Set<String> listInputDirectory() throws IOException {
        Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, "*"));
        return Stream.of(resources).map(Resource::getFilename).collect(Collectors.toSet());
    }
}
