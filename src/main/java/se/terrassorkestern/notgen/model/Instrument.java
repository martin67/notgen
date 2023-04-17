package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
public class Instrument {
    @Id
    private UUID id;

    @ManyToOne
    private Band band;

    @NotBlank(message = "Instrumentnamn måste anges")
    private String name;
    private String shortName;
    @NotNull(message = "Sorteringsordning måste anges")
    private Integer sortOrder;

    @ManyToMany(mappedBy = "instruments")
    private Set<Setting> settings = new HashSet<>();

    public Instrument() {
        this.id = UUID.randomUUID();
    }
}
