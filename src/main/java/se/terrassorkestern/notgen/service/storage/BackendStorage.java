package se.terrassorkestern.notgen.service.storage;

import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BackendStorage {

    Path downloadScore(Score score, Path location) throws IOException;

    Path downloadScorePart(ScorePart scorePart, Path location) throws IOException;

    Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException;

    boolean isScoreGenerated(Score score) throws IOException;

    void uploadScore(Score score, Path path) throws IOException;

    void uploadScorePart(ScorePart scorePart, Path path) throws IOException;

    OutputStream getCoverOutputStream(Score score) throws IOException;

    OutputStream getThumbnailOutputStream(Score score) throws IOException;

    void deleteScore(Score score) throws IOException;

    void deleteScoreParts(Score score) throws IOException;

    void cleanOutput() throws IOException;

    default String getScoreName(Score score) {
        return String.format("%d.pdf", score.getId());
    }

    default String getScorePartName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }

    default String getScorePartName(ScorePart scorePart) {
        return String.format("%d-%d.pdf", scorePart.getScore().getId(), scorePart.getInstrument().getId());
    }

    default String getCoverName(Score score) {
        return String.format("%d-cover.jpg", score.getId());
    }

    default String getThumbnailName(Score score) {
        return String.format("%d-thumbnail.png", score.getId());
    }
}
