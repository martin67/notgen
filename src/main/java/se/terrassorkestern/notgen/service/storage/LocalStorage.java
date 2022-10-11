package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
    public Path download(Score score, Path location) throws IOException {
        return Files.copy(input.resolve(score.getFilename()), location.resolve(score.getFilename()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path download(ScorePart scorePart, Path location) throws IOException {
        return Files.copy(output.resolve(scorePart.getPdfName()), location.resolve(scorePart.getPdfName()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path download(Score score, Instrument instrument, Path location) throws IOException {
        String filename = getScorePartName(score, instrument);
        return Files.copy(output.resolve(filename), location.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
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
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(output.resolve(String.valueOf(scorePart.getScore().getId())));
        Files.copy(path, scoreOutput.resolve(scorePart.getPdfName()), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void cleanOutput() {

    }

    private String getScorePartName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }
}
