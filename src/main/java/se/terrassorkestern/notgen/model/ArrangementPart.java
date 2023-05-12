package se.terrassorkestern.notgen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "arrangement_instrument")
public class ArrangementPart implements Comparable<ArrangementPart> {

    @EmbeddedId
    private ArrangementPartId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("arrangementId")
    private Arrangement arrangement;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("instrumentId")
    private Instrument instrument;

    private int page;
    private int length = 1;
    private String comment;


    public ArrangementPart() {
        this.id = new ArrangementPartId();
    }

    public ArrangementPart(Arrangement arrangement, Instrument instrument) {
        this.arrangement = arrangement;
        this.instrument = instrument;
        this.id = new ArrangementPartId(arrangement.getId(), instrument.getId());
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

    @Override
    public int compareTo(ArrangementPart o) {
        if (this.instrument == null && o.instrument == null) {
            return 0;
        }
        if (this.instrument == null) {
            return 1;
        }
        if (o.instrument == null) {
            return -1;
        }
        return Integer.compare(instrument.getSortOrder(), o.instrument.getSortOrder());
    }
}
