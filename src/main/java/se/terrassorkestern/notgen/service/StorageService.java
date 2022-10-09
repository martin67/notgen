package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
public class StorageService {

    private final Path input;
    private final Path output;
    private final Path tmp;

    public StorageService(@Value("${notgen.folders.input}") String inputDir,
                          @Value("${notgen.folders.output}") String outputDir,
                          @Value("${notgen.folders.tempdir}") String tempDir) {
        input = Path.of(inputDir);
        output = Path.of(outputDir);
        tmp = Path.of(tempDir);
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

    public Path downloadScore(Score score, Path location) throws IOException {
        // Start with local service
        return Files.copy(input.resolve(score.getFilename()), location.resolve(score.getFilename()), StandardCopyOption.REPLACE_EXISTING);
    }

    public void saveScorePart(ScorePart scorePart, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(output.resolve(String.valueOf(scorePart.getScore().getId())));
        Files.copy(path, scoreOutput.resolve(getFileName(scorePart)), StandardCopyOption.REPLACE_EXISTING);
    }

    boolean isScoreGenerated(Score score) {
        boolean allFilesExist = true;
        for (ScorePart scorePart : score.getScoreParts()) {
            if (!Files.exists(output.resolve(String.valueOf(scorePart.getScore().getId())).resolve(getFileName(scorePart)))) {
                allFilesExist = false;
            }
        }
        return allFilesExist;
    }

    public Path toPath(Score score, Instrument instrument) {
        return output.resolve(String.valueOf(score.getId())).resolve(getFileName(score, instrument));
    }

    private String getFileName(ScorePart scorePart) {
        return getFileName(scorePart.getScore(), scorePart.getInstrument());
    }

    private String getFileName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }

}
