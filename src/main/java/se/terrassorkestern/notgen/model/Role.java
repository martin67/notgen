package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Role {

    @Id
    private UUID id;

    private String name;
    private String displayName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_privilege", joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"))
    private Collection<Privilege> privileges;


    public Role(String name) {
        this.name = name;
        this.displayName = "";
        this.id = UUID.randomUUID();
    }

    public Role(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.id = UUID.randomUUID();
    }

}
