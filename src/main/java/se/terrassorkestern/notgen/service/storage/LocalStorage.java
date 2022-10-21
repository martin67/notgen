package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Storage on local disk.
 * <p>
 * The scores are organized in folders, one for each score
 *
 * <output_dir>/<score_id>/<score_id>-<instrument_id>.pdf
 * <p>
 * Example: /var/lib/notgen/output/488/488-12.pdf
 */
@Slf4j
@Service
public class LocalStorage implements BackendStorage {

    private final Path input;
    private final Path output;

    public LocalStorage(@Value("${notgen.storage.local.input}") String inputDir,
                        @Value("${notgen.storage.local.output}") String outputDir) {
        input = Path.of(inputDir);
        output = Path.of(outputDir);
    }

    @Override
    public Path downloadScore(Score score, Path location) throws IOException {
        return Files.copy(input.resolve(score.getFilename()), location.resolve(score.getFilename()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return Files.copy(output.resolve(String.valueOf(scorePart.getScore().getId())).resolve(scorePart.getPdfName()),
                location.resolve(scorePart.getPdfName()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        String filename = getScorePartName(score, instrument);
        return Files.copy(output.resolve(String.valueOf(score.getId())).resolve(filename),
                location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        boolean allFilesExist = true;
        for (ScorePart scorePart : score.getScoreParts()) {
            if (!Files.exists(output.resolve(String.valueOf(scorePart.getScore().getId())).resolve(scorePart.getPdfName()))) {
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
        Path scoreOutput = Files.createDirectories(output.resolve(String.valueOf(scorePart.getScore().getId())));
        Files.copy(path, scoreOutput.resolve(scorePart.getPdfName()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deleteScore(Score score) throws IOException {

    }

    @Override
    public void deleteScoreParts(Score score) throws IOException {
        FileSystemUtils.deleteRecursively(output.resolve(String.valueOf(score.getId())));
    }

    @Override
    public void cleanOutput() throws IOException {
        FileSystemUtils.deleteRecursively(output);
    }

    private String getScorePartName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }
}
