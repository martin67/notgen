package se.terrassorkestern.notgen.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import se.terrassorkestern.notgen.model.Statistics;

import java.io.StringWriter;
import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Disabled
@Sql({"/testdata/statistics.sql"})
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void getStatistics() {
        Statistics statistics = statisticsService.getStatistics();
        assertThat(statistics).isNotNull();
        assertThat(statistics.getNumberOfInstruments()).isEqualTo(3);
        assertThat(statistics.getNumberOfPlaylists()).isEqualTo(3);
        assertThat(statistics.getTopGenres()).hasSize(2);
        assertThat(statistics.getNumberOfSongs()).isEqualTo(3);
        assertThat(statistics.getNumberOfScannedSongs()).isEqualTo(1);
        assertThat(statistics.getNumberOfScannedPages()).isEqualTo(5);
    }

    @Test
    void writeScoresToCsv() {
        Writer writer = new StringWriter();
        statisticsService.writeScoresToCsv(writer);
        assertThat(writer.toString()).contains("Titel");
        assertThat(writer.toString()).hasLineCount(4);
    }

    @Test
    void writeFullScoresToCsv() {
        Writer writer = new StringWriter();
        statisticsService.writeFullScoresToCsv(writer);
        assertThat(writer.toString()).contains("Titel", "Instrument");
        assertThat(writer.toString()).hasLineCount(4);
    }

    @Test
    void writeUnscannedToCsv() {
        Writer writer = new StringWriter();
        statisticsService.writeUnscannedToCsv(writer);
        assertThat(writer.toString()).hasLineCount(2);
    }

}