package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Band {
    @Id
    private UUID id;
    private String name;
    private String description;

    public Band() {
        this.id = UUID.randomUUID();
    }

    public Band(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
    }
}