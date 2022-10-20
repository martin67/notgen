package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import se.terrassorkestern.notgen.model.Statistics;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Tag("manual")
@Sql({"/full-data.sql"})
class StatisticsServiceTest {

    @Autowired
    StatisticsService statisticsService;


    @Test
    @Disabled
    void getStatistics() {
        Statistics statistics = statisticsService.getStatistics();
        assertThat(statistics.getNumberOfInstruments()).isEqualTo(39L);
        assertThat(statistics.getNumberOfPlaylists()).isEqualTo(7L);
        assertThat(statistics.getTopGenres().size()).isEqualTo(5L);
        assertThat(statistics.getNumberOfSongs()).isEqualTo(434L);
        assertThat(statistics.getNumberOfScannedSongs()).isEqualTo(414L);
        assertThat(statistics.getNumberOfScannedPages()).isEqualTo(6346L);
    }
}