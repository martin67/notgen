package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import se.terrassorkestern.notgen.service.storage.BackendStorage;
import se.terrassorkestern.notgen.service.storage.LocalStorage;
import se.terrassorkestern.notgen.service.storage.S3Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class StorageService {

    private final Path tmp;
    private final BackendStorage backendStorage;

    public StorageService(@Value("${notgen.folders.tempdir}") String tempDir, @Value("${notgen.storage}") String storage,
                          S3Storage s3Storage, LocalStorage localStorage) {
        tmp = Path.of(tempDir);
        switch (storage) {
            case "s3":
                this.backendStorage = s3Storage;
                break;
            case "local":
                this.backendStorage = localStorage;
                break;
            default:
                throw new IllegalArgumentException("notgen.storage " + storage + " not valid");
        }
    }

    public Path getTmpDir(Score score) throws IOException {
        Path t;
        if (tmp != null) {
            t = Files.createDirectories(tmp.resolve("score-" + score.getId()));
            // Remove all files in directory
            FileUtils.cleanDirectory(t.toFile());
        } else {
            t = Files.createTempDirectory("notkonv-");
        }
        log.debug("Creating temporary directory {}", t.toString());
        return t;
    }

    public Path getTmpDir() throws IOException {
        Path t;
        if (tmp != null) {
            t = Files.createDirectories(tmp.resolve("tmp"));
            // Remove all files in directory
            FileUtils.cleanDirectory(t.toFile());
        } else {
            t = Files.createTempDirectory("notgen");
        }
        log.debug("Creating temporary directory {}", t.toString());
        return t;
    }

    public Path downloadScore(Score score, Path location) throws IOException {
        return backendStorage.download(score, location);
    }

    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        return backendStorage.download(score, instrument, location);
    }

    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return backendStorage.download(scorePart, location);
    }

    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        backendStorage.uploadScorePart(scorePart, path);
    }

    boolean isScoreGenerated(Score score) {
        return backendStorage.isScoreGenerated(score);
    }

}
