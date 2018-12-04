package se.terrassorkestern.notgen2;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Data
@Entity
@Table(name="sattning")
public class ScorePart implements Serializable {

    @Column(name="sida")
    private int page;
    @Column(name="antal")
    private int length;
    @Column(name="kommentar")
    private String comment;

    @Id
    @ManyToOne
    @JoinColumn(name="repertoire_id")
    private Song song;

    @Id
    @ManyToOne
    @JoinColumn(name="instrument_id")
//    @OrderBy(value="instrument.sortOrder")
    private Instrument instrument;


    ScorePart () {}

    @Override
    public String toString() {
        return "ScorePart{" +
                "page=" + page +
                ", length=" + length +
                ", comment='" + comment + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScorePart scorePart = (ScorePart) o;
        return Objects.equals(song, scorePart.song) &&
                Objects.equals(instrument, scorePart.instrument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(song, instrument);
    }
}
