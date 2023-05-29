package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class ConfigurationKey {
    @Id
    private UUID id;
    private String area;
    private String name;
    private String description;
    private String comment;

    public ConfigurationKey() {
        this.id = UUID.randomUUID();
    }
}
