package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.util.UUID;

@Slf4j
@Entity
@Data
@Table(name = "ngfile")
public class NgFile {
    @Id
    private UUID id;

    private String filename;
    private NgFileType type;
    private String name;
    private String originalFilename;
    private String comment;

    public NgFile() {
        this.id = UUID.randomUUID();
    }

    public void setFilename(String extension) {
        filename = String.format("%s.%s", id, extension);
    }

    public void setFullFilename(String filename) {
        this.filename = filename;
    }

    public MediaType getContentType() {
        String extension = com.google.common.io.Files.getFileExtension(filename);
        return switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

}
