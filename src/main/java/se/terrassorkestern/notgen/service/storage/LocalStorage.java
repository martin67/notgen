package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

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

    public LocalStorage(@Value("${notgen.storage.local.input}") String inputDir,
                        @Value("${notgen.storage.local.output}") String outputDir,
                        @Value("${notgen.folders.static}") String staticDir) {
        this.inputDir = Path.of(inputDir);
        this.outputDir = Path.of(outputDir);
        this.staticDir = Path.of(staticDir);
    }

    @Override
    public Path downloadScore(Score score, Path location) throws IOException {
        return Files.copy(inputDir.resolve(score.getFilename()), location.resolve(score.getFilename()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return Files.copy(outputDir.resolve(String.valueOf(scorePart.getScore().getId())).resolve(getScorePartName(scorePart)),
                location.resolve(getScorePartName(scorePart)), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        String filename = getScorePartName(score, instrument);
        return Files.copy(outputDir.resolve(String.valueOf(score.getId())).resolve(filename),
                location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        boolean allFilesExist = true;
        for (ScorePart scorePart : score.getScoreParts()) {
            if (!Files.exists(outputDir.resolve(String.valueOf(scorePart.getScore().getId())).resolve(getScorePartName(scorePart)))) {
                allFilesExist = false;
            }
        }
        return allFilesExist;
    }

    @Override
    public void uploadScore(Score score, Path path) throws IOException {

    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(outputDir.resolve(String.valueOf(scorePart.getScore().getId())));
        Files.copy(path, scoreOutput.resolve(getScorePartName(scorePart)), StandardCopyOption.REPLACE_EXISTING);
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

    @Override
    public void deleteScore(Score score) throws IOException {

    }

    @Override
    public void deleteScoreParts(Score score) throws IOException {
        FileSystemUtils.deleteRecursively(outputDir.resolve(String.valueOf(score.getId())));
    }

    @Override
    public void cleanOutput() throws IOException {
        FileSystemUtils.deleteRecursively(outputDir);
    }

}
