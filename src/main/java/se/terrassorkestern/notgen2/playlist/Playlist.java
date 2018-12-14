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
    @JoinColumn(name = "playlist_id", nullable = false)
    @OrderBy("sortOrder")
    List<PlaylistEntry> playlistEntries = new ArrayList<>();

    public Playlist copy() {
        Playlist newPlaylist = new Playlist();

        newPlaylist.setName("Kopia av " + this.getName());
        newPlaylist.setDate(this.getDate());
        newPlaylist.setComment(this.getComment());
        for(PlaylistEntry playlistEntry : this.getPlaylistEntries()) {
            PlaylistEntry newPlaylistEntry = new PlaylistEntry();
            newPlaylistEntry.setText(playlistEntry.getText());
            newPlaylistEntry.setComment(playlistEntry.getComment());
            newPlaylistEntry.setSortOrder(playlistEntry.getSortOrder());
            newPlaylist.getPlaylistEntries().add(newPlaylistEntry);
        }
        return newPlaylist;
    }
}
