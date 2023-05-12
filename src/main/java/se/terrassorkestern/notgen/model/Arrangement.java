package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Entity
@Getter
@Setter
public class Arrangement {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Score score;
    private String arranger;
    private String name;
    @Lob
    private String comment;
    @ManyToOne(fetch = FetchType.LAZY)
    private NgFile file;
    private ScoreType scoreType;
    private boolean cover = false;
    private String archiveLocation;

    public Arrangement() {
        this.id = UUID.randomUUID();
        this.name = "New arr";
    }

    public Arrangement(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    @OneToMany(mappedBy = "arrangement", cascade = CascadeType.ALL, orphanRemoval = true)
    private SortedSet<ArrangementPart> arrangementParts = new TreeSet<>();
    //private List<ArrangementPart> arrangementParts = new ArrayList<>();

    public void addArrangementPart(ArrangementPart arrangementPart) {
        arrangementParts.add(arrangementPart);
        arrangementPart.setArrangement(this);
    }

    public void removeArrangementPart(ArrangementPart arrangementPart) {
        arrangementParts.remove(arrangementPart);
        arrangementPart.setArrangement(null);
    }

    public List<Instrument> getInstruments() {
        List<Instrument> result = new ArrayList<>();
        for (ArrangementPart arrangementPart : arrangementParts) {
            result.add(arrangementPart.getInstrument());
        }
        return result;
    }

    @Override
    public String toString() {
        return name + " (" + id +")";
    }
}
