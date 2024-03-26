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
    private String publisher;
    private String name;
    @Lob
    private String comment;
    @ManyToOne(fetch = FetchType.LAZY)
    private NgFile file;
    private ScoreType scoreType;
    private boolean cover = false;
    private boolean adjustMargins = true;
    private String archiveLocation;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    private List<Configuration> configurations;

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
        for (var arrangementPart : arrangementParts) {
            result.add(arrangementPart.getInstrument());
        }
        return result;
    }

    public Optional<ArrangementPart> getArrangementPart(Instrument instrument)  {
        return arrangementParts.stream().filter(arrangementPart -> arrangementPart.getInstrument() == instrument).findFirst();
    }

    public String getConfig(String key, String defaultValue) {
        return configurations.stream()
                .filter(c -> c.getKey().getToken().equals(key))
                .map(Configuration::getVal)
                .findFirst().orElse(defaultValue);
    }

    public int getConfig(String key, int defaultValue) {
        return configurations.stream()
                .filter(c -> c.getKey().getToken().equals(key))
                .map(Configuration::getVal)
                .mapToInt(Integer::parseInt)
                .findFirst().orElse(defaultValue);
    }

    public double getConfig(String key, double defaultValue) {
        return configurations.stream()
                .filter(c -> c.getKey().getToken().equals(key))
                .map(Configuration::getVal)
                .mapToDouble(Double::parseDouble)
                .findFirst().orElse(defaultValue);
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
