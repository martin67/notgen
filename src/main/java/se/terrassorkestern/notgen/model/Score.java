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

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Arrangement> arrangements = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Arrangement defaultArrangement;

    @OneToMany(mappedBy ="score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NgFile> files = new ArrayList<>();

    @OneToMany(mappedBy ="score", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Link> links = new ArrayList<>();
    private boolean linksPresent = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="item_id", referencedColumnName = "id")
    private List<Configuration> configurations;


    public Score() {
        this.id = UUID.randomUUID();
    }

    public Score(Band band, String title) {
        this.id = UUID.randomUUID();
        this.band = band;
        this.title = title;
    }

    public void addArrangement(Arrangement arrangement) {
        arrangements.add(arrangement);
        arrangement.setScore(this);
    }

    public void removeArrangement(Arrangement arrangement) {
        arrangements.remove(arrangement);
        arrangement.setScore(null);
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

    public Link getLink(UUID id) {
        return links.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Link %s not found", id)));
    }

    public String getThumbnailPath() {
        return (defaultArrangement != null && defaultArrangement.isCover()) ? String.format("/%s-thumbnail.png", defaultArrangement.getId()) : "/images/thoreehrling.jpg";
    }

    public String getCoverPath() {
        return (defaultArrangement != null && defaultArrangement.isCover()) ? String.format("/%s-cover.jpg", defaultArrangement.getId()) : "/images/thoreehrling.jpg";
    }

    public NgFile getFile(UUID fileId) {
        return files.stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("File %s not found", fileId)));
    }

    public void addFile(NgFile file) {
        files.add(file);
        file.setScore(this);
    }

    public void removeFile(NgFile file) {
        files.remove(file);
        file.setScore(null);
    }

    public void removeFile(UUID uuid) {
        removeFile(getFile(uuid));
    }

    public void addLink(Link link) {
        links.add(link);
        linksPresent = true;
        link.setScore(this);
    }

    public void removeLink(Link link) {
        links.remove(link);
        if (links.isEmpty()) {
            linksPresent = false;
        }
        link.setScore(null);
    }

    public void removeLink(UUID uuid) {
        removeLink(getLink(uuid));
    }

    @Override
    public String toString() {
        return title + " (" + id +")";
    }
}
