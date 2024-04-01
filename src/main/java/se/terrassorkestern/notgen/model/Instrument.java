package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
public class Instrument implements Comparable<Instrument> {
    @Id
    private UUID id;

    @ManyToOne
    private Band band;

    @NotBlank(message = "Instrumentnamn måste anges")
    private String name;
    private String shortName;
    @NotNull(message = "Sorteringsordning måste anges")
    private int sortOrder;
    private boolean song = false;

    @ManyToMany(mappedBy = "instruments")
    private Set<Setting> settings = new HashSet<>();

    public Instrument() {
        this.id = UUID.randomUUID();
    }

    public Instrument(Band band, String name, String shortName, int sortOrder) {
        this.id = UUID.randomUUID();
        this.band = band;
        this.name = name;
        this.shortName = shortName;
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }

    @Override
    public int compareTo(Instrument o) {
        return Integer.compare(sortOrder, o.sortOrder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instrument that = (Instrument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
