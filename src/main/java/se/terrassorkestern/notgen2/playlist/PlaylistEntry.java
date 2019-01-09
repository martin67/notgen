package se.terrassorkestern.notgen2.playlist;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="song_playlist")
public class PlaylistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="sortorder")
    private Integer sortOrder;
    private String text;
    private Boolean bold;
    private String comment;
}
