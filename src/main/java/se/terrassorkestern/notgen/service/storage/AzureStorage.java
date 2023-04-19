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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    @Override
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        String fileName = arrangement.getFile().getFilename();
        Path destination = location.resolve(fileName);
        Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
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
            String pattern = String.format("%s-*.pdf", arrangement.getId());
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
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        if (file.getOriginalFilename() == null) {
            throw new StorageException("No file name set");
        }

        try (InputStream inputStream = file.getInputStream()) {
            NgFile ngFile = new NgFile();
            String extension = com.google.common.io.Files.getFileExtension(file.getOriginalFilename());
            ngFile.setFilename(extension);
            upload(ngFile.getFilename(), inputStream, scoreContainer);
            return ngFile;
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public void deleteFile(String filename) throws StorageException {
        log.error("Delete ({}) not implemented yet!", filename);
    }

    @Override
    public void renameFile(NgFile file, String newName) throws IOException {
        upload(newName, downloadFile(file), scoreContainer);
    }

    @Override
    public void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException {
        log.info("Uploading arrangement part, score {} ({}), arr: {} ({}), instrument: {} ({})",
                arrangementPart.getArrangement().getScore().getTitle(),
                arrangementPart.getArrangement().getScore().getId(),
                arrangementPart.getArrangement().getName(),
                arrangementPart.getArrangement().getId(),
                arrangementPart.getInstrument().getName(),
                arrangementPart.getInstrument().getId());
        String fileName = getArrangementPartName(arrangementPart);
        Resource storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scorePartsContainer, fileName));
        try (OutputStream os = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, os);
            log.debug("write data to container={}, fileName={}", scorePartsContainer, fileName);
        }
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
        if (file == null || file.getFilename() == null) {
            throw new StorageException("No file name set");
        }
        BufferedInputStream bis;
        try {
            String fileName = file.getFilename();
            Resource resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
            bis = new BufferedInputStream(resource.getInputStream());
        } catch (IOException e) {
            throw new StorageException("Could not open file");
        }
        return bis;
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
    public void cleanOutput() {

    }

    @Override
    public Set<String> listInputDirectory() throws IOException {
        Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, "*"));
        return Stream.of(resources).map(Resource::getFilename).collect(Collectors.toSet());
    }
}
