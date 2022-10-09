package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.Statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql({"/full-data.sql"})
class StatisticsServiceTest {

    @Autowired
    StatisticsService statisticsService;


    @Test
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