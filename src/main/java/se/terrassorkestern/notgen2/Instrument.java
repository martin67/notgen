package se.terrassorkestern.notgen2;

import javax.persistence.*;

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

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isStandard() {
        return standard;
    }

    void setStandard(boolean standard) {
        this.standard = standard;
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
