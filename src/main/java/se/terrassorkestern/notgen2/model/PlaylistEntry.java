package se.terrassorkestern.notgen2.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "score_playlist")
public class PlaylistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Integer sortOrder;
    private String text;
    private Boolean bold;
    private String comment;
}
