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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;
    private UUID uuid;

    private String name;
    private String description;

    public Band() {
        this.uuid = UUID.randomUUID();
    }
}