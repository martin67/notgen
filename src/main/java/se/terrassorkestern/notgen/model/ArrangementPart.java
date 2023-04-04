package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "arrangement_instrument")
public class ArrangementPart {

    @EmbeddedId
    private ArrangementPartId id;

    @ManyToOne
    @MapsId("arrangementId")
    private Arrangement arrangement;

    @ManyToOne
    @MapsId("instrumentId")
    @OrderBy("sortOrder")
    private Instrument instrument;
    private int page;
    private int length = 1;
    private String comment;


    public ArrangementPart() {
    }

    public ArrangementPart(ScorePart scorePart) {
        this.instrument = scorePart.getInstrument();
        this.page = scorePart.getPage();
        this.length = scorePart.getLength();
        this.comment = scorePart.getComment();
    }

    @Override
    public String toString() {
        return "ArrangementPart{"
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
        ArrangementPart that = (ArrangementPart) o;
        return Objects.equals(arrangement, that.arrangement)
                && Objects.equals(instrument, that.instrument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrangement, instrument);
    }
}
