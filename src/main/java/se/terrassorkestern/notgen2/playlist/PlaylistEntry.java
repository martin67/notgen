package se.terrassorkestern.notgen2.playlist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "song_playlist")
public class PlaylistEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @Column(name = "sortorder")
  private Integer sortOrder;
  private String text;
  private Boolean bold;
  private String comment;
}
