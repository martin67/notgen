package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
public class Configuration {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private ConfigurationKey key;
    private String val;

    public Configuration() {
        this.id = UUID.randomUUID();
    }
}
