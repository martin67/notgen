package se.terrassorkestern.notgen2;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="repertoire_playlist")
public class PlaylistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JoinColumn(name="repertoire_id")
    private Song song;

    @Column(name="sortorder")
    private Integer sortOrder;
    private String text;
}
