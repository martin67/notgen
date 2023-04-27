package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Arrangement {
    @Id
    private UUID id;
    private String arranger;
    private String name;
    @Lob
    private String comment;
    @ManyToOne(fetch = FetchType.LAZY)
    private NgFile file;
    @ManyToOne
    private Score score;
    private ScoreType scoreType;
    private Boolean cover = false;
    private String archiveLocation;

    public Arrangement() {
        this.id = UUID.randomUUID();
        this.name = "New arr";
    }

    @OneToMany(mappedBy = "arrangement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ArrangementPart> arrangementParts = new ArrayList<>();

    public void addArrangementPart(ArrangementPart arrangementPart) {
        arrangementPart.setArrangement(this);
        arrangementParts.add(arrangementPart);
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
