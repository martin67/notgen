package se.terrassorkestern.notgen2.song;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

// See doc at https://vladmihalcea.com/the-best-way-to-map-a-many-to-many-association-with-extra-columns-when-using-jpa-and-hibernate/
@Embeddable
public class ScorePartId implements Serializable {
    @Column(name = "song_id")
    private int songId;

    @Column(name = "instrument_id")
    private int instrumentId;

    private ScorePartId() {
    }

    public ScorePartId(
            int songId,
            int instrumentId) {
        this.songId = songId;
        this.instrumentId = instrumentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ScorePartId that = (ScorePartId) o;
        return Objects.equals(songId, that.songId) &&
                Objects.equals(instrumentId, that.instrumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(songId, instrumentId);
    }
}
