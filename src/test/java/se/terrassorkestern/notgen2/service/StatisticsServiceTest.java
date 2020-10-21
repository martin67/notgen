package se.terrassorkestern.notgen2.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.terrassorkestern.notgen2.model.Statistics;

import javax.transaction.Transactional;

@SpringBootTest
//@DataJpaTest
@Transactional
class StatisticsServiceTest {

    @Autowired
    StatisticsService statisticsService;


    @Test
    void getStatistics() {
        Statistics statistics = statisticsService.getStatistics();

        long hej = statistics.getNumberOfSongs();
    }
}