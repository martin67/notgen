package se.terrassorkestern.notgen2;

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


    public Instrument() {}

    public Instrument(int id, String name, String shortName, int sortOrder, boolean standard) {
        this.setId(id);
        this.setName(name);
        this.setShortName(shortName);
        this.setSortOrder(sortOrder);
        this.setStandard(standard);
    }

    public Instrument(String name, String shortName, int sortOrder, boolean standard) {
        this.setName(name);
        this.setShortName(shortName);
        this.setSortOrder(sortOrder);
        this.setStandard(standard);
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                ", sortOrder=" + sortOrder +
                ", standard=" + standard +
                '}';
    }
}
