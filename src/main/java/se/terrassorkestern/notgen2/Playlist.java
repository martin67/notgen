package se.terrassorkestern.notgen2;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name="playlist")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name="namn")
    private String name;
    @Column(name="kommentar")
    private String comment;
    @Column(name="datum")
    private LocalDate date;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "playlist_id")
    List<PlaylistEntry> playlistEntries = new ArrayList<>();
}
