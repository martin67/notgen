package se.terrassorkestern.notgen2;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "repertoire")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "titel")
    private String title;
    @Column(name = "subtitel")
    private String subtitle;
    @Column(name = "genre")
    private String genre;
    @Column(name = "musik")
    private String composer;
    @Column(name = "text")
    private String author;
    @Column(name = "arr")
    private String arranger;
    @Column(name = "ar")
    private Integer year;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScorePart> scoreParts;

    @Column(name = "inscannad")
    private Boolean scanned;
    @Column(name = "omslag")
    private Boolean cover;
    @Column(name = "bildbehandla")
    private Boolean imageProcess;
    private Boolean upperleft;
    private Boolean color;

    @Column(name = "filnamn")
    private String filename;


    public Song() {
    }


    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", genre='" + genre + '\'' +
                ", composer='" + composer + '\'' +
                ", author='" + author + '\'' +
                ", arranger='" + arranger + '\'' +
                ", year=" + year +
                '}';
    }
}
