package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private UUID uuid;

    private String name;
    @ManyToMany(mappedBy = "privileges")
    private Collection<Role> roles;


    public Privilege(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

}
