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
    private String token;
    private String name;
    private String description;
    private String comment;

    public ConfigurationKey() {
        this.id = UUID.randomUUID();
    }

    public ConfigurationKey(String area, String token, String name, String description, String comment) {
        this.id = UUID.randomUUID();
        this.area = area;
        this.token = token;
        this.name = name;
        this.description = description;
        this.comment = comment;
    }
}
