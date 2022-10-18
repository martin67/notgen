package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class AzureStorage implements BackendStorage {
    @Override
    public Path downloadScore(Score score, Path location) throws IOException {
        return null;
    }

    @Override
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return null;
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
        return null;
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        return false;
    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {

    }

    @Override
    public void deleteScoreParts(Score score) throws IOException {

    }

    @Override
    public void cleanOutput() throws IOException {

    }
}
