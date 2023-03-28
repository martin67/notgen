package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import se.terrassorkestern.notgen.service.storage.AzureStorage;
import se.terrassorkestern.notgen.service.storage.BackendStorage;
import se.terrassorkestern.notgen.service.storage.LocalStorage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class StorageService {

    private static final int BUFFER_SIZE = 4096;
    private final BackendStorage backendStorage;
    private final String tempDir;
    private final boolean keepTempDir;

    public StorageService(@Value("${notgen.keep.tempdir:false}") boolean keepTempDir,
                          @Value("${notgen.storage.type}") String storage,
                          @Value("${notgen.folders.temp:}") String tempDir,
                          AzureStorage azureStorage, LocalStorage localStorage) {
        this.keepTempDir = keepTempDir;
        this.tempDir = tempDir;

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
            t = Files.createTempDirectory("notgen");
        } else {
            t = Files.createTempDirectory(Path.of(tempDir), "notgen");
        }
        log.debug("Creating temporary directory {}", t);
        return t;
    }

    public Path createTempDir(Score score) throws IOException {
        Path t;
        if (tempDir.isEmpty()) {
            t = Files.createTempDirectory("notgen");
        } else {
            t = Files.createDirectories(Path.of(tempDir).resolve(score.getTitle()).resolve(String.valueOf(Instant.now().getEpochSecond())));
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

    public Path downloadScore(Score score, Path location) throws IOException {
        return backendStorage.downloadScore(score, location);
    }

    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        return backendStorage.downloadScorePart(score, instrument, location);
    }

    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return backendStorage.downloadScorePart(scorePart, location);
    }

    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        backendStorage.uploadScorePart(scorePart, path);
    }

    public boolean isScoreGenerated(Score score) throws IOException {
        return backendStorage.isScoreGenerated(score);
    }

    public Path replaceExtension(Path path, String newExtension) {
        Path parent = path.getParent();
        String fileName = path.getFileName().toString();
        return parent.resolve(com.google.common.io.Files.getNameWithoutExtension(fileName) + newExtension);
    }

    public OutputStream getCoverOutputStream(Score score) throws IOException {
        return backendStorage.getCoverOutputStream(score);
    }

    public OutputStream getThumbnailOutputStream(Score score) throws IOException {
        return backendStorage.getThumbnailOutputStream(score);
    }

    public void extractZip(Path zipFile, Path dir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile.toFile())), Charset.forName("CP437"))
        ) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    log.warn("zip {} contains a directory {}", zipFile, zipEntry.getName());
                } else {
                    // if the entry is a file, extracts it
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dir.resolve(zipEntry.getName()).toFile()))) {
                        byte[] bytesIn = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = zipInputStream.read(bytesIn)) != -1) {
                            bos.write(bytesIn, 0, read);
                        }
                    }
                }
            }
        }
    }
}
