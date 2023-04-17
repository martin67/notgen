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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private UUID uuid;

    @ManyToOne
    private Band band;
    private UUID band_uuid;

    private String name;
    @ManyToMany
    @JoinTable(
            name = "setting_instrument",
            joinColumns = @JoinColumn(name = "setting_id"),
            inverseJoinColumns = @JoinColumn(name = "instrument_id"))
    private Set<Instrument> instruments = new HashSet<>();

    public Setting() {
        this.uuid = UUID.randomUUID();
    }
}
