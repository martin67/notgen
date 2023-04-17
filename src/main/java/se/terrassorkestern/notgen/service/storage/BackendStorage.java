package se.terrassorkestern.notgen.service.storage;

import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.exceptions.StorageException;
import se.terrassorkestern.notgen.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;

public interface BackendStorage {

    Path downloadArrangement(Arrangement arrangement, Path location) throws IOException;

    Path downloadArrangementPart(Arrangement arrangement, Instrument instrument, Path location) throws IOException;

    boolean isScoreGenerated(Score score) throws IOException;

    void uploadArrangement(Arrangement arrangement, Path path) throws IOException;

    NgFile uploadFile(MultipartFile file) throws StorageException;

    InputStream downloadFile(NgFile file) throws StorageException;

    void deleteFile(String filename) throws StorageException;

    void uploadArrangementPart(ArrangementPart arrangementPart, Path path) throws IOException;

    OutputStream getCoverOutputStream(Score score) throws IOException;

    OutputStream getThumbnailOutputStream(Score score) throws IOException;

    Path renameScore(Score score, String newName) throws IOException;

    void cleanOutput() throws IOException;

    default String getArrangementName(Arrangement arrangement, String extension) {
        return String.format("%d-%d.%s", arrangement.getScore().getId(),
                arrangement.getId(), extension);
    }

    default String getArrangementPartName(ArrangementPart arrangementPart) {
        return String.format("%s-%s.pdf", arrangementPart.getArrangement().getId(), arrangementPart.getInstrument().getId());
    }

    default String getArrangementPartName(Arrangement arrangement, Instrument instrument) {
        ArrangementPart arrangementPart = arrangement.getArrangementParts().stream()
                .filter(ap -> ap.getInstrument() == instrument)
                .findFirst().orElseThrow();
        return getArrangementPartName(arrangementPart);
    }

    default String getCoverName(Score score) {
        return String.format("%d-cover.jpg", score.getId());
    }

    default String getThumbnailName(Score score) {
        return String.format("%d-thumbnail.png", score.getId());
    }

    Set<String> listInputDirectory() throws IOException;
}
