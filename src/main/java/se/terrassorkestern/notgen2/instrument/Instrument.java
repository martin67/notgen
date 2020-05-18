package se.terrassorkestern.notgen2.instrument;

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
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotBlank(message = "Instrumentnamn måste anges")
    private String name;
    private String shortName;
    @NotNull(message = "Sorteringsordning måste anges")
    private Integer sortOrder;

    @ManyToMany(mappedBy = "instruments")
    private Set<Setting> settings = new HashSet<>();
}
