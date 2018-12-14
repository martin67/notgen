package se.terrassorkestern.notgen2.song;

import lombok.Data;
import se.terrassorkestern.notgen2.instrument.Instrument;

import javax.persistence.*;
import java.util.Objects;

@Data
@Entity
@Table(name="sattning")
public class ScorePart {

    @EmbeddedId
    private ScorePartId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instrumentId")
    @OrderBy("sortOrder")
    private Instrument instrument;

    @Column(name="sida")
    private int page;
    @Column(name="antal")
    private int length = 1;
    @Column(name="kommentar")
    private String comment;

/* Behövs inte om man kör standard
    @Id
    @ManyToOne
    @JoinColumn(name="repertoire_id")
    private Song song;
*/

/*
    @Id
    @ManyToOne
    @JoinColumn(name="instrument_id")
    private Instrument instrument;
*/

// Nytt försök

    public ScorePart () {}

    public ScorePart(Song song, Instrument instrument) {
        this.song = song;
        this.instrument = instrument;
        this.id = new ScorePartId(song.getId(), instrument.getId());
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
        ScorePart that = (ScorePart) o;
        return Objects.equals(song, that.song) &&
                Objects.equals(instrument, that.instrument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(song, instrument);
    }
}
