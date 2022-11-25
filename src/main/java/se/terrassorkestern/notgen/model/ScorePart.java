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

    @ManyToOne
    @MapsId("scoreId")
    private Score score;

    @ManyToOne
    @MapsId("instrumentId")
    @OrderBy("sortOrder")
    private Instrument instrument;
    private int page;
    private int length = 1;
    private String comment;


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
