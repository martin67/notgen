package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Band {
    @Id
    private UUID id;
    @NotBlank(message = "Bandnamn måste anges")
    private String name;
    private String description;
    @OneToOne
    private Setting standardSetting;

    public Band() {
        this.id = UUID.randomUUID();
    }

    public Band(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
    }
}