package se.terrassorkestern.notgen2.instrument;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "instrument")
public class Instrument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  @Column(name = "namn")
  @NotBlank(message = "Instrumentnamn måste anges")
  private String name;
  @Column(name = "forkortning")
  private String shortName;
  @Column(name = "sortorder")
  @NotNull(message = "Sorteringsordning måste anges")
  private Integer sortOrder;
  private boolean standard;

}
