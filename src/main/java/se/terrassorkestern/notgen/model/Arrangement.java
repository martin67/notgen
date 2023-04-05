package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Arrangement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String arranger;
    private String name;
    @Lob
    private String comment;
    @ManyToOne(fetch = FetchType.LAZY)
    private NgFile file;
    @ManyToOne
    private Score score;

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
