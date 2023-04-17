package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import se.terrassorkestern.notgen.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 Relations:

   Score - Arrangement - ArrangementPart - Instrument
              arranger       page
              comment        length
                             comment
 */

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
    private UUID uuid;
    @ManyToOne
    private Band band;

    @NotBlank(message = "Titel måste anges")
    @FullTextField(analyzer = "swedish")
    private String title;
    @FullTextField(analyzer = "swedish")
    private String subTitle;
    //@NotBlank(message = "Genre måste anges")
    @FullTextField(analyzer = "swedish")
    private String genre = "Foxtrot";
    @FullTextField(analyzer = "swedish")
    private String composer;
    @FullTextField(analyzer = "swedish")
    private String author;
    @FullTextField(analyzer = "swedish")
    private String arranger;
    @Column(name = "year_")             // year is a reserved name in H2...
    private Integer year = 1940;
    @FullTextField(analyzer = "swedish")
    private String publisher;
    @Lob
    @FullTextField(analyzer = "swedish")
    private String comment;
    @Lob
    private String internalComment;
    @Lob
    @FullTextField(analyzer = "swedish")
    private String text;
    @Lob
    private String presentation;

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Arrangement> arrangements = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    private Arrangement defaultArrangement;
    private UUID defaultArrangement_uuid;

    @OneToMany(cascade = CascadeType.ALL)
    private List<NgFile> files = new ArrayList<>();

    private ScoreType scoreType;
    private Boolean scanned = true;
    private Boolean cover = true;
    private Boolean imageProcess = true;
    private Boolean upperleft = true;
    private Boolean color = true;

    private Boolean archived = true;
    private String archiveLocation = "A";

    private String filename;

//    public List<Instrument> getInstruments() {
//        List<Instrument> result = new ArrayList<>();
//        for (ScorePart scorePart : scoreParts) {
//            result.add(scorePart.getInstrument());
//        }
//        return result;
//    }

    public Score() {
        this.uuid = UUID.randomUUID();
    }

    public void addArrangement(Arrangement arrangement) {
        arrangement.setScore(this);
        arrangements.add(arrangement);
    }

    public Arrangement getArrangement(String name) {
        return arrangements.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Arrangement %s not found", name)));
    }

    public String getThumbnailPath() {
        return (cover != null && cover) ? String.format("/%d-thumbnail.png", id) : "/images/thoreehrling.jpg";
    }

    public String getCoverPath() {
        return (cover != null && cover) ? String.format("/%d-cover.jpg", id) : "/images/thoreehrling.jpg";
    }

    public NgFile getFile(int fileId) {
        return files.stream()
                .filter(f -> f.getId() == fileId)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("File %d not found", fileId)));
    }

    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
}
