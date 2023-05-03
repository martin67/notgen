package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
public class Setting {
    @Id
    private UUID id;

    @ManyToOne
    private Band band;

    private String name;
    @ManyToMany
    @JoinTable(
            name = "setting_instrument",
            joinColumns = @JoinColumn(name = "setting_id"),
            inverseJoinColumns = @JoinColumn(name = "instrument_id"))
    private Set<Instrument> instruments = new HashSet<>();

    public Setting() {
        this.id = UUID.randomUUID();
    }

    public Setting(Band band, String name) {
        this.id = UUID.randomUUID();
        this.band = band;
        this.name = name;
    }
}
