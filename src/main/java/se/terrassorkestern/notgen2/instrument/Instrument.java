package se.terrassorkestern.notgen2.instrument;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Data
@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "namn")
    @NotBlank(message = "Instrumentnamn måste anges")
    String name;
    @Column(name = "forkortning")
    private String shortName;
    @Column(name = "sortorder")
    @NotBlank(message = "Sorteringsordning måste anges")
    private Integer sortOrder;
    private boolean standard;

}
