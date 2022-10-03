package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Service
public class StorageService {

    @Value("${notgen.folders.tempdir}")
    private String tempDir;

    private static final Path INPUT = Paths.get("/Work/TO/notgen/score_input");
    private static final Path OUTPUT = Paths.get("/Work/TO/notgen/score_output");

    Path getTmpDir(Score score) throws IOException {
        Path t;
        if (tempDir != null) {
            t = Files.createDirectories(Paths.get(tempDir).resolve("score-" + score.getId()));
            //
        } else {
            t = Files.createTempDirectory("notkonv-");
        }
        log.debug("Creating temporary directory {}", t.toString());
        return t;
    }

    Path downloadScore(Score score, Path location) throws IOException {
        // Start with local service
        return Files.copy(INPUT.resolve(score.getFilename()), location.resolve(score.getFilename()), StandardCopyOption.REPLACE_EXISTING);
    }

    void saveScorePart(ScorePart scorePart, Path path) throws IOException {
        Path scoreOutput = Files.createDirectories(OUTPUT.resolve(String.valueOf(scorePart.getScore().getId())));
        Files.copy(path, scoreOutput.resolve(getFileName(scorePart)), StandardCopyOption.REPLACE_EXISTING);
    }

    boolean isScoreGenerated(Score score) {
        boolean allFilesExist = true;
        for (ScorePart scorePart : score.getScoreParts()) {
            if (!Files.exists(OUTPUT.resolve(String.valueOf(scorePart.getScore().getId())).resolve(getFileName(scorePart)))) {
                allFilesExist = false;
            }
        }
        return allFilesExist;
    }

    public Path toPath(Score score, Instrument instrument) {
        return OUTPUT.resolve(String.valueOf(score.getId())).resolve(getFileName(score, instrument));
    }

    private String getFileName(ScorePart scorePart) {
        return getFileName(scorePart.getScore(), scorePart.getInstrument());
    }

    private String getFileName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }
}
