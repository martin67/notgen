package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
