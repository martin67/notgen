package se.terrassorkestern.notgen.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Indexed
@Getter
@Setter
@Table(name = "score", indexes = {
        @Index(name = "idx_title", columnList = "title"),
        @Index(name = "idx_band", columnList = "band_id")
})
public class Score extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    private Band band;
    @NotBlank(message = "Titel måste anges")
    @FullTextField()
    private String title;
    @FullTextField()
    private String subTitle;
    @NotBlank(message = "Genre måste anges")
    @FullTextField()
    private String genre = "Foxtrot";
    @FullTextField()
    private String composer;
    @FullTextField()
    private String author;
    @FullTextField()
    private String arranger;
    @Column(name = "year_")             // year is a reserved name in H2...
    private Integer year = 1940;
    @FullTextField()
    private String publisher;
    @Lob
    @FullTextField()
    private String comment;
    @Lob
    private String internalComment;
    @Lob
    @FullTextField()
    private String text;
    @Lob
    private String presentation;

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScorePart> scoreParts = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "score_id")
    private Set<Imagedata> imageData = new HashSet<>();

    private ScoreType scoreType;
    private Boolean scanned = true;
    private Boolean cover = true;
    private Boolean imageProcess = true;
    private Boolean upperleft = true;
    private Boolean color = true;

    private Boolean archived = true;
    private String archiveLocation = "A";

    private String filename;

    public List<Instrument> getInstruments() {
        List<Instrument> result = new ArrayList<>();
        for (ScorePart scorePart : scoreParts) {
            result.add(scorePart.getInstrument());
        }
        return result;
    }

    public String getThumbnailPath() {
        return (cover != null && cover) ? String.format("/%d-thumbnail.png", id) : "/images/thoreehrling.jpg";
    }

    public String getCoverPath() {
        return (cover != null && cover) ? String.format("/%d-cover.jpg", id) : "/images/thoreehrling.jpg";
    }

    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
}
