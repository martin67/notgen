package se.terrassorkestern.notgen2.playlist;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="repertoire_playlist")
public class PlaylistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name="sortorder")
    private Integer sortOrder;
    private String text;
    private String comment;
}
