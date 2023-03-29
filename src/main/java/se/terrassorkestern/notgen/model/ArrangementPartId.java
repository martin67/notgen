package se.terrassorkestern.notgen.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

// See doc at https://vladmihalcea.com/the-best-way-to-map-a-many-to-many-association-with-extra-columns-when-using-jpa-and-hibernate/
@Getter
@Setter
@Embeddable
public class ArrangementPartId implements Serializable {

    private int arrangementId;
    private int instrumentId;

    public ArrangementPartId() {
    }

    public ArrangementPartId(int arrangementId, int instrumentId) {
        this.arrangementId = arrangementId;
        this.instrumentId = instrumentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrangementPartId that = (ArrangementPartId) o;
        return Objects.equals(arrangementId, that.arrangementId)
                && Objects.equals(instrumentId, that.instrumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrangementId, instrumentId);
    }
}
