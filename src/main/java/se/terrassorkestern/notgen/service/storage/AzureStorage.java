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
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.NgFile;

import javax.annotation.Nullable;
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
    @Value("${se.terrassorkestern.notgen.storage.input:scores}")
    private String scoreContainer;
    @Value("${se.terrassorkestern.notgen.storage.output:arrangementParts}")
    private String arrangementPartsContainer;
    @Value("${se.terrassorkestern.notgen.storage.static:static}")
    private String staticContainer;


    public AzureStorage(@Qualifier("azureStorageBlobProtocolResolver") ResourceLoader resourceLoader,
                        AzureStorageBlobProtocolResolver patternResolver
    ) {
        this.resourceLoader = resourceLoader;
        this.azureStorageBlobProtocolResolver = patternResolver;
    }

    @Override
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        var fileName = arrangement.getFile().getFilename();
        var destination = location.resolve(fileName);
        var resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        var fileName = getArrangementPartFilename(arrangement, instrument);
        var destination = location.resolve(arrangement.getId() + "-" + instrument.getId() + ".pdf");
        var resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, arrangementPartsContainer, fileName));
        Files.copy(resource.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    @Override
    public boolean isArrangementGenerated(Arrangement arrangement) throws IOException {
        // check that all arrangement parts are generated
        boolean allGenerated = true;
        var pattern = String.format("%s-*.pdf", arrangement.getId());
        Resource[] resources = azureStorageBlobProtocolResolver.getResources(String.format(BLOB_RESOURCE_PATTERN, arrangementPartsContainer, pattern));
        // Just check that there are equal number of files as there should be score parts.
        if (resources.length != arrangement.getArrangementParts().size()) {
            allGenerated = false;
        }
        return allGenerated;
    }

    @Override
    public NgFile uploadFile(@Nullable MultipartFile file) throws StorageException {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        if (file.getOriginalFilename() == null) {
            throw new StorageException("No file name set");
        }

        try (InputStream inputStream = file.getInputStream()) {
            var ngFile = new NgFile();
            var extension = com.google.common.io.Files.getFileExtension(file.getOriginalFilename());
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
        // In Azure, all arrangements are stored in one container and need to have unique file names. These are on the
        // form arrangementUUID-instrumentUUID.pdf
        log.info("Uploading arrangement part, score {}, arr: {}, instrument: {}",
                arrangementPart.getArrangement().getScore(),
                arrangementPart.getArrangement(),
                arrangementPart.getInstrument());
        var fileName = getArrangementPartFilename(arrangementPart);
        var storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, arrangementPartsContainer, fileName));
        try (var outputStream = ((WritableResource) storageBlobResource).getOutputStream()) {
            Files.copy(path, outputStream);
            log.debug("write data to container={}, fileName={}", arrangementPartsContainer, fileName);
        }
    }

    private void upload(String fileName, InputStream inputStream, String container) throws IOException {
        var storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, container, fileName));
        try (var outputStream = ((WritableResource) storageBlobResource).getOutputStream()) {
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
            var fileName = file.getFilename();
            var resource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, scoreContainer, fileName));
            bis = new BufferedInputStream(resource.getInputStream());
        } catch (IOException e) {
            throw new StorageException("Could not open file");
        }
        return bis;
    }

    @Override
    public OutputStream getCoverOutputStream(Arrangement arrangement) throws IOException {
        var storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, staticContainer, getCoverName(arrangement)));
        return ((WritableResource) storageBlobResource).getOutputStream();
    }

    @Override
    public OutputStream getThumbnailOutputStream(Arrangement arrangement) throws IOException {
        var storageBlobResource = resourceLoader.getResource(String.format(BLOB_RESOURCE_PATTERN, staticContainer, getThumbnailName(arrangement)));
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

    private String getArrangementPartFilename(ArrangementPart arrangementPart) {
        return getArrangementPartFilename(arrangementPart.getArrangement(), arrangementPart.getInstrument());
    }

    private String getArrangementPartFilename(Arrangement arrangement, Instrument instrument) {
        return arrangement.getId() + "-" + instrument.getId() + ".pdf";
    }
}
