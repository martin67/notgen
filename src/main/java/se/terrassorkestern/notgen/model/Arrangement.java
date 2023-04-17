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
    private UUID file_uuid;

    @ManyToOne
    private Score score;
    private UUID score_uuid;

    public Arrangement() {
        this.id = UUID.randomUUID();
    }

    public Arrangement(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "arrangement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArrangementPart> arrangementParts = new ArrayList<>();

    public void addArrangementPart(ArrangementPart arrangementPart) {
        arrangementPart.setArrangement(this);
        arrangementPart.setId(new ArrangementPartId(this.id, arrangementPart.getInstrument().getId()));
        arrangementParts.add(arrangementPart);
    }

    public List<Instrument> getInstruments() {
        List<Instrument> result = new ArrayList<>();
        for (ArrangementPart arrangementPart : arrangementParts) {
            result.add(arrangementPart.getInstrument());
        }
        return result;
    }
}
