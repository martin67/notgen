package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Privilege {

    @Id
    private UUID id;

    private String name;
    @ManyToMany(mappedBy = "privileges")
    private Collection<Role> roles;


    public Privilege(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
    }

}
