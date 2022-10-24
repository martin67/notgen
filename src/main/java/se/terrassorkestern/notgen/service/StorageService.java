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
import se.terrassorkestern.notgen.service.storage.S3Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class StorageService {

    private final BackendStorage backendStorage;
    private Path tmp = null;

    public StorageService(@Value("${notgen.folders.tempdir:notset}") String tempDir, @Value("${notgen.storage}") String storage,
                          S3Storage s3Storage, AzureStorage azureStorage, LocalStorage localStorage) {

        if (!tempDir.equals("notset")) {
            tmp = Path.of(tempDir);
        }
        switch (storage) {
            case "s3" -> this.backendStorage = s3Storage;
            case "azure" -> this.backendStorage = azureStorage;
            case "local" -> this.backendStorage = localStorage;
            default -> throw new IllegalArgumentException("notgen.storage " + storage + " not valid");
        }
        log.info("Using storage: {}", storage);
    }

    public Path getTmpDir(Score score) throws IOException {
        Path t;
        if (tmp != null) {
            Path dir = tmp.resolve("score-" + score.getId());
            FileSystemUtils.deleteRecursively(dir);
            t = Files.createDirectories(dir);
        } else {
            t = Files.createTempDirectory("notkonv-");
        }
        log.debug("Creating temporary directory {}", t.toString());
        return t;
    }

    public Path getTmpDir() throws IOException {
        Path t;
        if (tmp != null) {
            Path dir = tmp.resolve("tmp");
            FileSystemUtils.deleteRecursively(dir);
            t = Files.createDirectories(dir);
        } else {
            t = Files.createTempDirectory("notgen");
        }
        log.debug("Creating temporary directory {}", t.toString());
        return t;
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

    boolean isScoreGenerated(Score score) throws IOException {
        return backendStorage.isScoreGenerated(score);
    }

}
