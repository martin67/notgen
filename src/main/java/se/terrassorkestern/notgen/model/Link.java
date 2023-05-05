package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.UUID;

@Getter
@Setter
@Entity
public class Link {
    @Id
    private UUID id;
    private URI uri;
    private LinkType type;
    private String name;
    private String comment;

    public Link() {
        this.id = UUID.randomUUID();
    }

    public Link(String uri, LinkType type, String name, String comment) {
        this.id = UUID.randomUUID();
        this.uri = URI.create(uri);
        this.type = type;
        this.name = name;
        this.comment = comment;
    }
}
