package se.terrassorkestern.notgen.model;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@Entity
public class Playlist extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private UUID uuid;

    @ManyToOne
    private Band band;
    private UUID band_uuid;

    @NotBlank(message = "Låtlistan måste ha ett namn")
    private String name;
    private String comment;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    @ManyToOne
    private Setting setting;
    private UUID setting_uuid;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "playlist_id", nullable = false)
    @OrderBy("sortOrder")
    private List<PlaylistEntry> playlistEntries = new ArrayList<>();

    public Playlist() {
        this.uuid = UUID.randomUUID();
    }

    public Playlist copy() {
        Playlist newPlaylist = new Playlist();

        newPlaylist.setName("Kopia av " + this.getName());
        newPlaylist.setDate(this.getDate());
        newPlaylist.setComment(this.getComment());
        newPlaylist.setBand(this.getBand());
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

    public long numberOfSongs() {
        return playlistEntries.stream().filter(playlistEntry -> !playlistEntry.getBold()).count();
    }
}
