package se.terrassorkestern.notgen.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScorePartIdTest {

    @Test
    void testEquals() {
        ScorePartId scorePartId1 = new ScorePartId();
        scorePartId1.setScoreId(1);
        scorePartId1.setInstrumentId(2);

        ScorePartId scorePartId2 = scorePartId1;
        assertThat(scorePartId2).isEqualTo(scorePartId1);

        ScorePartId scorePartId3 = new ScorePartId();
        scorePartId3.setScoreId(1);
        scorePartId3.setInstrumentId(2);
        assertThat(scorePartId2).isEqualTo(scorePartId1);

    }
}