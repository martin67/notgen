package se.terrassorkestern.notgen.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "song")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String subTitle;
    private String composer;
    private Integer year_;              // year is a reserved name in H2...
    private String comment;
}
