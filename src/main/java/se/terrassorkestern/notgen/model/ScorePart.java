package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "score_instrument")
public class ScorePart {

    @EmbeddedId
    private ScorePartId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("scoreId")
    private Score score;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instrumentId")
    @OrderBy("sortOrder")
    private Instrument instrument;
    private int page;
    private int length = 1;
    private String comment;
    private String googleId;
  

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

    public ScorePart() {
    }

    public ScorePart(Score score, Instrument instrument) {
        this.score = score;
        this.instrument = instrument;
        this.id = new ScorePartId(score.getId(), instrument.getId());
    }

    @Override
    public String toString() {
        return "ScorePart{"
                + "page=" + page
                + ", length=" + length
                + ", comment='" + comment + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScorePart that = (ScorePart) o;
        return Objects.equals(score, that.score)
                && Objects.equals(instrument, that.instrument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, instrument);
    }
}
