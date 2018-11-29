package se.terrassorkestern.notgen2;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

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
