package se.terrassorkestern.notgen2.playlist;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import se.terrassorkestern.notgen2.Auditable;
import se.terrassorkestern.notgen2.instrument.Setting;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
public class Playlist extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotBlank(message = "Låtlistan måste ha ett namn")
    private String name;
    private String comment;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @ManyToOne
    private Setting setting;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "playlist_id", nullable = false)
    @OrderBy("sortOrder")
    List<PlaylistEntry> playlistEntries = new ArrayList<>();

    //@Transient
    private LocalDateTime updated;

    Playlist copy() {
        Playlist newPlaylist = new Playlist();

        newPlaylist.setName("Kopia av " + this.getName());
        newPlaylist.setDate(this.getDate());
        newPlaylist.setComment(this.getComment());
        for (PlaylistEntry playlistEntry : this.getPlaylistEntries()) {
            PlaylistEntry newPlaylistEntry = new PlaylistEntry();
            newPlaylistEntry.setText(playlistEntry.getText());
            newPlaylistEntry.setBold(playlistEntry.getBold());
            newPlaylistEntry.setComment(playlistEntry.getComment());
            newPlaylistEntry.setSortOrder(playlistEntry.getSortOrder());
            newPlaylist.getPlaylistEntries().add(newPlaylistEntry);
        }
        return newPlaylist;
    }
}
