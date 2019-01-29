package se.terrassorkestern.notgen2.playlist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "playlist")
public class Playlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  @Column(name = "namn")
  @NotBlank(message = "Låtlistan måste ha ett namn")
  private String name;
  @Column(name = "kommentar")
  private String comment;
  @Column(name = "datum")
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
