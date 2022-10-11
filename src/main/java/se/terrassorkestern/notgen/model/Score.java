package se.terrassorkestern.notgen.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "score")
public class Score extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    private Song song;
    @NotBlank(message = "Titel måste anges")
    private String title;
    private String subTitle;
    @NotBlank(message = "Genre måste anges")
    private String genre = "Foxtrot";
    private String composer;
    private String author;
    private String arranger;
    @Column(name = "year_")             // year is a reserved name in H2...
    private Integer year = 1940;
    private String publisher;
    @Lob
    private String comment;
    @Lob
    private String internalComment;

    private String googleIdFull;
    private String googleIdTo;
    private String googleIdCover;
    private String googleIdThumbnail;

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScorePart> scoreParts = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "score_id")
    private List<Imagedata> imageData = new ArrayList<>();

    private Boolean scanned = true;
    private Boolean cover = true;
    private Boolean imageProcess = true;
    private Boolean upperleft = true;
    private Boolean color = true;

    private Boolean archived = true;
    private String archiveLocation = "A";

    private String filename;

    // Stuff to remove; first from DB then here
    private Boolean aktiv;
    private Boolean standard;
    private LocalDateTime inforskaffad;
    private String movie;
    private String orgText;
    private Integer betyg;

    public List<Instrument> getInstruments() {
        List<Instrument> result = new ArrayList<>();
        for (ScorePart scorePart : scoreParts) {
            result.add(scorePart.getInstrument());
        }
        return result;
    }

    public String getThumbnailPath() {
        return cover ? String.format("/thumbnails/%d.png", id) : "/images/thoreehrling.jpg";
    }

    public String getCoverPath() {
        return cover ? String.format("/covers/%d-cover.jpg", id) : "/images/thoreehrling.jpg";
    }

}