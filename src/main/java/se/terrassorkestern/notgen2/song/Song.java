package se.terrassorkestern.notgen2.song;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "song")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "titel")
    @NotBlank(message="Titel måste anges")
    private String title;
    @Column(name = "subtitel")
    private String subtitle;
    @Column(name = "genre")
    @NotBlank(message="Genre måste anges")
    private String genre = "Foxtrot";
    @Column(name = "musik")
    private String composer;
    @Column(name = "text")
    private String author;
    @Column(name = "arr")
    private String arranger;
    @Column(name = "ar")
    private Integer year = 1940;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScorePart> scoreParts = new ArrayList<>();

    @Column(name = "inscannad")
    private Boolean scanned = true;
    @Column(name = "omslag")
    private Boolean cover = true;
    @Column(name = "bildbehandla")
    private Boolean imageProcess = true;
    private Boolean upperleft = true;
    private Boolean color = true;

    @Column(name = "filnamn")
    private String filename;

    @Column(name = "orginal")
    private Boolean archived = true;
    @Column(name = "Arkivplats")
    private String archiveLocation = "A";
}
