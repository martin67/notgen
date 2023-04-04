package se.terrassorkestern.notgen.service.storage;

import se.terrassorkestern.notgen.model.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface BackendStorage {

    Path downloadArrangement(Arrangement arrangement, Path location) throws IOException;

    Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException;

    boolean isScoreGenerated(Score score) throws IOException;

    void uploadArrangement(Arrangement arrangement, Path path) throws IOException;

    void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException;

    OutputStream getCoverOutputStream(Score score) throws IOException;

    OutputStream getThumbnailOutputStream(Score score) throws IOException;

    Path renameScore(Score score, String newName) throws IOException;

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

    default String getArrangementName(Arrangement arrangement, String extension) {
        return String.format("%d-%d.%s", arrangement.getScore().getId(),
                arrangement.getId(), extension);
    }

    default String getArrangementPartName(ArrangementPart arrangementPart) {
        return String.format("%d-%d-%d.pdf", arrangementPart.getArrangement().getScore().getId(),
                arrangementPart.getArrangement().getId(), arrangementPart.getInstrument().getId());
    }

    default String getArrangementPartName(Arrangement arrangement, Instrument instrument) {
        return String.format("%d-%d-%d.pdf", arrangement.getScore().getId(),
                arrangement.getId(), instrument.getId());
    }

    default String getCoverName(Score score) {
        return String.format("%d-cover.jpg", score.getId());
    }

    default String getThumbnailName(Score score) {
        return String.format("%d-thumbnail.png", score.getId());
    }
}
