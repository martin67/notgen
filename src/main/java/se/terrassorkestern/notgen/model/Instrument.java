package se.terrassorkestern.notgen.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;


/**
 * DTO for Instrument
 */
@Getter
@Setter
@Entity
@Table(name = "instrument")
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    @NotBlank(message = "Instrumentnamn måste anges")
    private String name;
    private String shortName;
    @NotNull(message = "Sorteringsordning måste anges")
    private Integer sortOrder;

    @ManyToMany(mappedBy = "instruments")
    private Set<Setting> settings = new HashSet<>();
}
