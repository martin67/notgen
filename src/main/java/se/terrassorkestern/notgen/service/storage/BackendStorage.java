package se.terrassorkestern.notgen.service.storage;

import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.nio.file.Path;

public interface BackendStorage {

    Path downloadScore(Score score, Path location) throws IOException;

    Path downloadScorePart(ScorePart scorePart, Path location) throws IOException;

    Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException;

    boolean isScoreGenerated(Score score);

    void uploadScorePart(ScorePart scorePart, Path path) throws IOException;

    void deleteScoreParts(Score score) throws IOException;

    void cleanOutput() throws IOException;
}
