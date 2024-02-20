package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.NgFileRepository;
import se.terrassorkestern.notgen.service.storage.AzureStorage;
import se.terrassorkestern.notgen.service.storage.LocalStorage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {StorageService.class, LocalStorage.class},
        properties = {"e.terrassorkestern.notgen.storage.type=local"})
class StorageServiceTest {

    @Autowired
    private StorageService storageService;
    @MockBean
    private NgFileRepository ngFileRepository;
    @MockBean
    private AzureStorage azureStorage;
//    @Autowired
//    private LocalStorage localStorage;

    @Test
    void createTempDir() throws IOException {
        var path = storageService.createTempDir();
        assertThat(path).exists();
        storageService.deleteTempDir(path);

        path = storageService.createTempDir(new Score());
        assertThat(path).exists();
        storageService.deleteTempDir(path);
    }

    @Test
    void deleteTempDir() throws IOException {
        var path = storageService.createTempDir();
        assertThat(path).exists();
        storageService.deleteTempDir(path);
        assertThat(path).doesNotExist();
    }

    @Test
    void downloadArrangement() {
    }

    @Test
    void downloadArrangementPart() {
    }

    @Test
    void uploadFile() {
    }

    @Test
    void downloadFile() {
    }

    @Test
    void uploadArrangementPart() {
    }

    @Test
    void isScoreGenerated() {
    }

    @Test
    void isArrangementGenerated() {
    }

    @Test
    void replaceExtension() {
        var result = storageService.replaceExtension(Path.of("/bla/test.foo"), ".bar");
        assertThat(result).hasFileName("test.bar");
        result = storageService.replaceExtension(Path.of("bla/test.foo"), ".bar");
        assertThat(result).hasFileName("test.bar");
        result = storageService.replaceExtension(Path.of("test.foo"), ".bar");
        assertThat(result).hasFileName("test.bar");
    }

    @Test
    void getCoverOutputStream() {
    }

    @Test
    void getThumbnailOutputStream() {
    }

    @Test
    void extractZip() throws IOException, URISyntaxException {
        var path = Path.of(getClass().getClassLoader().getResource("testdata/1057.zip").toURI());
        var tempDir = storageService.createTempDir();

        storageService.extractZip(path, tempDir);
        assertThat(tempDir).isNotEmptyDirectory();

        storageService.deleteTempDir(tempDir);
    }

    @Test
    void cleanupInput() {
    }
}