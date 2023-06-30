package se.terrassorkestern.notgen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "score_playlist")
public class PlaylistEntry {
    @Id
    private UUID id;
    private int sortOrder;
    private String text;
    private boolean bold;
    private String comment;

    public PlaylistEntry() {
        this.id = UUID.randomUUID();
    }

    public PlaylistEntry(int sortOrder, String text, boolean bold, String comment) {
        this.id = UUID.randomUUID();
        this.sortOrder = sortOrder;
        this.text = text;
        this.bold = bold;
        this.comment = comment;
    }
}
