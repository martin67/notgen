package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.net.URI;
import java.util.UUID;

@Entity
@Data
public class Link {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    private Score score;
    private URI uri;
    private String uri2;
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

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
