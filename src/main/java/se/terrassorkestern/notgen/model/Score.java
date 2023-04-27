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
    private UUID id;
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

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Arrangement> arrangements = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Arrangement defaultArrangement;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<NgFile> files = new ArrayList<>();


    public Score() {
        this.id = UUID.randomUUID();
    }

    public void addArrangement(Arrangement arrangement) {
        arrangement.setScore(this);
        arrangements.add(arrangement);
    }

    public Arrangement getArrangement(UUID id) {
        return arrangements.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Arrangement %s not found", id)));
    }

    public Arrangement getArrangement(String id) {
        return arrangements.stream()
                .filter(a -> a.getId().toString().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Arrangement %s not found", id)));
    }

    public String getThumbnailPath() {
        return (defaultArrangement != null && defaultArrangement.getCover()) ? String.format("/%s-thumbnail.png", defaultArrangement.getId()) : "/images/thoreehrling.jpg";
    }

    public String getCoverPath() {
        return (defaultArrangement != null && defaultArrangement.getCover()) ? String.format("/%s-cover.jpg", defaultArrangement.getId()) : "/images/thoreehrling.jpg";
    }

    public NgFile getFile(UUID fileId) {
        return files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("File %s not found", fileId)));
    }

    @Override
    public String toString() {
        return title + " (" + id +")";
    }
}
