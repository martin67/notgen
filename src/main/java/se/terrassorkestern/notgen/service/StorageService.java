package se.terrassorkestern.notgen.service;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.exceptions.StorageException;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.NgFileRepository;
import se.terrassorkestern.notgen.service.storage.AzureStorage;
import se.terrassorkestern.notgen.service.storage.BackendStorage;
import se.terrassorkestern.notgen.service.storage.LocalStorage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class StorageService {

    private static final int BUFFER_SIZE = 4096;
    public static final String TEMPDIR_PREFIX = "notgen";
    private final BackendStorage backendStorage;
    private final String tempDir;
    private final NgFileRepository ngFileRepository;
    private final boolean keepTempDir;

    public StorageService(@Value("${se.terrassorkestern.notgen.storage.keeptemp:false}") boolean keepTempDir,
                          @Value("${se.terrassorkestern.notgen.storage.type}") String storage,
                          @Value("${se.terrassorkestern.notgen.storage.temp:}") String tempDir,
                          NgFileRepository ngFileRepository,
                          AzureStorage azureStorage, LocalStorage localStorage) {
        this.keepTempDir = keepTempDir;
        this.tempDir = tempDir;
        this.ngFileRepository = ngFileRepository;

        switch (storage) {
            case "azure" -> this.backendStorage = azureStorage;
            case "local" -> this.backendStorage = localStorage;
            default -> throw new IllegalArgumentException("notgen.storage.type " + storage + " not valid");
        }
        log.info("Using storage: {}", storage);
    }

    public Path createTempDir() throws IOException {
        Path t;
        if (tempDir.isEmpty()) {
            t = Files.createTempDirectory(TEMPDIR_PREFIX);
        } else {
            t = Files.createTempDirectory(Path.of(tempDir), TEMPDIR_PREFIX);
        }
        log.debug("Creating temporary directory {}", t);
        return t;
    }

    public Path createTempDir(Score score) throws IOException {
        Path t;
        if (tempDir.isEmpty()) {
            t = Files.createTempDirectory(TEMPDIR_PREFIX);
        } else {
            t = Files.createDirectories(Path.of(tempDir)
                    .resolve(score.getTitle().replaceAll("[?]", "_"))
                    .resolve(String.valueOf(Instant.now().getEpochSecond())));
        }
        log.debug("Creating temporary directory {}", t);
        return t;
    }

    public void deleteTempDir(Path tempDir) throws IOException {
        if (!keepTempDir) {
            log.debug("Deleting temporary directory {}", tempDir);
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        return backendStorage.downloadArrangement(arrangement, location);
    }

    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        return backendStorage.downloadArrangementPart(arrangement, instrument, location);
    }

    public NgFile uploadFile(MultipartFile file) throws StorageException {
        var ngFile = backendStorage.uploadFile(file);
        ngFile.setOriginalFilename(file.getOriginalFilename());
        return ngFile;
    }

    public InputStream downloadFile(NgFile file) throws StorageException {
        return backendStorage.downloadFile(file);
    }

    public void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException {
        backendStorage.uploadArrangementPart(arrangementPart, path);
    }

    public boolean isScoreGenerated(Score score) throws IOException {
        boolean allArrangementsGenerated = true;

        for (var arrangement : score.getArrangements()) {
            if (arrangement.getFile() != null && !backendStorage.isArrangementGenerated(arrangement)) {
                allArrangementsGenerated = false;
            }
        }
        return allArrangementsGenerated;
    }

    public boolean isArrangementGenerated(Arrangement arrangement) throws IOException {
        return backendStorage.isArrangementGenerated(arrangement);
    }

    public Path replaceExtension(Path path, String newExtension) {
        var parent = path.getParent();
        var newFileName = com.google.common.io.Files.getNameWithoutExtension(path.getFileName().toString()) + newExtension;
        if (parent == null) {
            return Path.of(newFileName);
        } else {
            return parent.resolve(newFileName);
        }
    }

    public OutputStream getCoverOutputStream(Arrangement arrangement) throws IOException {
        return backendStorage.getCoverOutputStream(arrangement);
    }

    public OutputStream getThumbnailOutputStream(Arrangement arrangement) throws IOException {
        return backendStorage.getThumbnailOutputStream(arrangement);
    }

    public void extractZip(Path zipFile, Path dir) throws IOException {
        try (var zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile.toFile())), Charset.forName("CP437"))
        ) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    log.warn("zip {} contains a directory {}", zipFile, zipEntry.getName());
                } else {
                    // if the entry is a file, extracts it
                    try (var bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(dir.resolve(zipEntry.getName()).toFile()))) {
                        byte[] bytesIn = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = zipInputStream.read(bytesIn)) != -1) {
                            bufferedOutputStream.write(bytesIn, 0, read);
                        }
                    }
                }
            }
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    public void cleanupInput() throws IOException {
        // List all files in input directory
        var existingFiles = backendStorage.listInputDirectory();

        // List all files that should be in the input directory
        Set<String> ngFiles = new HashSet<>();
        ngFileRepository.findAll().forEach(ngFile -> ngFiles.add(ngFile.getFilename()));

        Set<String> diff = Sets.difference(existingFiles, ngFiles);

        // Remove the excess. These are files that are removed or uploaded but not saved.
        if (diff.size() > 100) {
            log.warn("Not deleting {} files!", diff.size());
            return;
        }

        if (!diff.isEmpty()) {
            log.info("Cleanup of input directory. Removing {} files", diff.size());
            for (String file : diff) {
                log.debug("Deleting {}", file);
                backendStorage.deleteFile(file);
            }
        }
    }
}
