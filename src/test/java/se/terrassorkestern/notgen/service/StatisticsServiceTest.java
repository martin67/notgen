package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import se.terrassorkestern.notgen.model.Statistics;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql({"/minimal-test-data.sql"})
class StatisticsServiceTest {

    @Autowired
    StatisticsService statisticsService;


    @Test
    void getStatistics() {
        Statistics statistics = statisticsService.getStatistics();

        assertThat(statistics.getNumberOfInstruments()).isEqualTo(39L);
        assertThat(statistics.getNumberOfPlaylists()).isEqualTo(0L);
        assertThat(statistics.getTopGenres().size()).isEqualTo(0);
    }
}