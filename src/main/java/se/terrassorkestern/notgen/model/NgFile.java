package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "file")
public class NgFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    private String filename;
    private NgFileType type;
    private String name;
    private String originalFilename;
    private String comment;
}
