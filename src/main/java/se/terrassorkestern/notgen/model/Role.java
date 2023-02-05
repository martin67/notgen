package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String displayName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_privilege", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"))
    private Collection<Privilege> privileges;


    public Role(String name) {
        this.name = name;
        this.displayName = "";
    }

    public Role(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

}
