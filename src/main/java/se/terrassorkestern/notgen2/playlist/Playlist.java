package se.terrassorkestern.notgen2.playlist;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;
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
    @NotBlank(message="Låtlistan måste ha ett namn")
    private String name;
    @Column(name="kommentar")
    private String comment;
    @Column(name="datum")
    @PastOrPresent(message="Ange ett datum")
    private LocalDate date;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "playlist_id")
    List<PlaylistEntry> playlistEntries = new ArrayList<>();
}
