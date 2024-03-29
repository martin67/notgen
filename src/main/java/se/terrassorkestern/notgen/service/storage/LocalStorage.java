package se.terrassorkestern.notgen.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import se.terrassorkestern.notgen.exceptions.StorageException;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.NgFile;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Storage on local disk.
 * <p>
 * The scores are organized in folders, one for each score
 * <p>
 * <output_dir>/<score_id>/<score_id>-<instrument_id>.pdf
 * <p>
 * Example: /var/lib/notgen/output/488/488-12.pdf
 */
@Slf4j
@Service
public class LocalStorage implements BackendStorage {

    private final Path inputDir;
    private final Path outputDir;
    private final Path staticDir;

    public LocalStorage(@Value("${se.terrassorkestern.notgen.storage.input}") String inputDir,
                        @Value("${se.terrassorkestern.notgen.storage.output}") String outputDir,
                        @Value("${se.terrassorkestern.notgen.storage.content}") String staticDir) {
        this.inputDir = Path.of(inputDir);
        this.outputDir = Path.of(outputDir);
        this.staticDir = Path.of(staticDir);
    }

    @Override
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        var filename = arrangement.getFile().getFilename();
        return Files.copy(inputDir.resolve(filename), location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        var source = getArrangementPartPath(arrangement, instrument);
        var destination = location.resolve(arrangement.getId() + "-" + instrument.getId() + ".pdf");
        return Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean isArrangementGenerated(Arrangement arrangement) {
        boolean allPartsGenerated = true;

        for (var arrangementPart : arrangement.getArrangementParts()) {
            if (!Files.exists(getArrangementPartPath(arrangementPart))) {
                allPartsGenerated = false;
            }
        }
        return allPartsGenerated;
    }

    @Override
    public NgFile uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }
        if (file.getOriginalFilename() == null) {
            throw new StorageException("No file name set");
        }

        try (var inputStream = file.getInputStream()) {
            var ngFile = new NgFile();
            var extension = com.google.common.io.Files.getFileExtension(file.getOriginalFilename());
            ngFile.setFilename(extension);
            Files.copy(inputStream, inputDir.resolve(ngFile.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            return ngFile;
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public InputStream downloadFile(NgFile file) throws StorageException {
        if (file == null || file.getFilename() == null) {
            throw new StorageException("No file name set");
        }
        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(new FileInputStream(inputDir.resolve(file.getFilename()).toFile()));
        } catch (FileNotFoundException ex) {
            throw new StorageException("Could not download file");
        }
        return bis;
    }

    @Override
    public void deleteFile(String filename) throws StorageException {
        try {
            Files.delete(inputDir.resolve(filename));
        } catch (IOException e) {
            throw new StorageException("Could not delete file " + filename);
        }
    }

    @Override
    public void renameFile(NgFile file, String newName) throws IOException {
        Files.move(inputDir.resolve(file.getFilename()), inputDir.resolve(newName));
        file.setFullFilename(newName);
    }

    @Override
    public void uploadArrangementPart(ArrangementPart arrangementPart, Path source) throws IOException {
        // ArrangementParts are stored in the hierarchy  score / arrangement / arrangementPart
        // The arrangement part filename is the instrument UUID value
        var destination = getArrangementPartPath(arrangementPart);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public OutputStream getCoverOutputStream(Arrangement arrangement) throws IOException {
        var outputPath = staticDir.resolve(getCoverName(arrangement));
        return Files.newOutputStream(outputPath);
    }

    @Override
    public OutputStream getThumbnailOutputStream(Arrangement arrangement) throws IOException {
        var outputPath = staticDir.resolve(getThumbnailName(arrangement));
        return Files.newOutputStream(outputPath);
    }

    @Override
    public void cleanOutput() throws IOException {
        FileSystemUtils.deleteRecursively(outputDir);
    }

    @Override
    public Set<String> listInputDirectory() throws IOException {
        try (var stream = Files.list(inputDir)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    private Path getArrangementPartPath(ArrangementPart arrangementPart) {
        return getArrangementPartPath(arrangementPart.getArrangement(), arrangementPart.getInstrument());
    }

    private Path getArrangementPartPath(Arrangement arrangement, Instrument instrument) {
        return outputDir
                .resolve(arrangement.getScore().getId().toString())
                .resolve(arrangement.getId().toString())
                .resolve(instrument.getId().toString() + ".pdf");
    }
}
