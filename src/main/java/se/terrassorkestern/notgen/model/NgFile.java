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
    @ManyToOne(fetch = FetchType.LAZY)
    private Score score;
    private String filename;
    private NgFileType type;
    private String name;
    private String originalFilename;
    private String comment;

    public NgFile() {
        this.id = UUID.randomUUID();
    }

    public NgFile(String filename, NgFileType type, String name, String originalFilename, String comment) {
        this.id = UUID.randomUUID();
        this.filename = filename;
        this.type = type;
        this.name = name;
        this.originalFilename = originalFilename;
        this.comment = comment;
    }

    public void setFilename(String extension) {
        filename = String.format("%s.%s", id, extension);
    }

    public void setFullFilename(String filename) {
        this.filename = filename;
    }

    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : originalFilename;
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
