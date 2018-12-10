package se.terrassorkestern.notgen2.instrument;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="instrument")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name="namn")
    private String name;
    @Column(name="forkortning")
    private String shortName;
    @Column(name="sortorder")
    private Integer sortOrder;
    private boolean standard;

}
