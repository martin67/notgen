package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "score_playlist")
public class PlaylistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private UUID uuid;

    private Integer sortOrder;
    private String text;
    private Boolean bold;
    private String comment;

    public PlaylistEntry() {
        this.uuid = UUID.randomUUID();
    }
}
