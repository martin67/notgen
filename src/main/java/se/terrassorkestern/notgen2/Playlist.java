package se.terrassorkestern.notgen2;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.TemporalType.DATE;

@Data
@Entity
@Table(name="playlist")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name="namn")
    private String name;
    @Column(name="kommentar")
    private String comment;

/*
    @Column(name="datum")
    private LocalDate date;
*/

    @Temporal(DATE)
    @DateTimeFormat(pattern="YYYY-mm-dd")
    @Column(name="datum")
    private Date date;

}
