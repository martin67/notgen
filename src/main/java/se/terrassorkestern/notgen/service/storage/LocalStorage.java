package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.exceptions.StorageException;
import se.terrassorkestern.notgen.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public LocalStorage(@Value("${notgen.storage.input}") String inputDir,
                        @Value("${notgen.storage.output}") String outputDir,
                        @Value("${notgen.folders.static}") String staticDir) {
        this.inputDir = Path.of(inputDir);
        this.outputDir = Path.of(outputDir);
        this.staticDir = Path.of(staticDir);
    }

    @Override
    public Path downloadArrangement(Arrangement arrangement, Path location) throws IOException {
        String filename = arrangement.getFile().getFilename();
        return Files.copy(inputDir.resolve(filename), location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException {
        String filename = getArrangementPartName(arrangement, instrument);
        return Files.copy(outputDir.resolve(String.valueOf(arrangement.getScore().getId())).resolve(filename),
                location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        // check that all arrangements are generated
        boolean allGenerated = true;

        for (Arrangement arrangement : score.getArrangements()) {
            for (ArrangementPart arrangementPart : arrangement.getArrangementParts()) {
                if (!Files.exists(outputDir.resolve(String.valueOf(score.getId())).resolve(getArrangementPartName(arrangementPart)))) {
                    allGenerated = false;
                }
            }
        }
        return allGenerated;
    }

    @Override
    public NgFile uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }
            if (file.getOriginalFilename() == null) {
                throw new StorageException("No file name set");
            }

            NgFile ngFile = new NgFile();

            String extension = com.google.common.io.Files.getFileExtension(file.getOriginalFilename());
            ngFile.setFilename(extension);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, inputDir.resolve(ngFile.getFilename()), StandardCopyOption.REPLACE_EXISTING);
            }
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
            bis = new BufferedInputStream(new FileInputStream(inputDir.resolve(file.getFilename()).
                    toFile()));
        } catch (FileNotFoundException e) {
            throw new StorageException("Could not find file");
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
    public void uploadArrangement(Arrangement arrangement, Path path) throws IOException {
        String extension = com.google.common.io.Files.getFileExtension(path.getFileName().toString());
        Files.copy(path, inputDir.resolve(getArrangementName(arrangement, extension)), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(outputDir.resolve(String.valueOf(arrangementPart.getArrangement().getScore().getId())));
        Files.copy(path, scoreOutput.resolve(getArrangementPartName(arrangementPart)), StandardCopyOption.REPLACE_EXISTING);
    }

    public void uploadCover(Score score, InputStream inputStream) throws IOException {
        Path scoreOutput = Files.createDirectories(outputDir.resolve(String.valueOf(score.getId())));
        Files.copy(inputStream, scoreOutput.resolve(getCoverName(score)), StandardCopyOption.REPLACE_EXISTING);
    }

    public void uploadThumbnail(Score score, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(outputDir.resolve(String.valueOf(score.getId())));
        Files.copy(path, scoreOutput.resolve(getThumbnailName(score)), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public OutputStream getCoverOutputStream(Score score) throws IOException {
        Path outputPath = staticDir.resolve(getCoverName(score));
        return Files.newOutputStream(outputPath);
    }

    @Override
    public OutputStream getThumbnailOutputStream(Score score) throws IOException {
        Path outputPath = staticDir.resolve(getThumbnailName(score));
        return Files.newOutputStream(outputPath);
    }

    // old file name: scoreid-1.xyz, new fileId.xyz
    @Override
    public Path renameScore(Score score, String newName) throws IOException {
        String fileName = String.format("%d-1", score.getId());
        String[] files = inputDir.toFile().list((d, name) -> name.startsWith(fileName));
        if (files == null || files.length == 0) {
            log.error("No files found for pattern {}", fileName);
            return null;
        } else if (files.length > 1) {
            log.warn("Multiple resources found for pattern {}, using the first", fileName);
        }
        //files[0]
        String extension = com.google.common.io.Files.getFileExtension(files[0]);
        Path newPath = inputDir.resolve(newName + "." + extension);
        log.debug("Moving {} to {}", inputDir.resolve(files[0]), newPath);
        //return newPath;
        return Files.move(inputDir.resolve(files[0]), newPath);
    }

    @Override
    public void cleanOutput() throws IOException {
        FileSystemUtils.deleteRecursively(outputDir);
    }

    @Override
    public Set<String> listInputDirectory() throws IOException {
        try (Stream<Path> stream = Files.list(inputDir)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

}
