package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

}
