package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = StatisticsService.class)
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private PlaylistRepository playlistRepository;


    @BeforeEach
    void setup() {
        given(scoreRepository.count()).willReturn(5L);
        given(scoreRepository.numberOfArrangements()).willReturn(6L);
        given(instrumentRepository.count()).willReturn(7L);
        given(playlistRepository.count()).willReturn(8L);

        var band = new Band();
        given(scoreRepository.findByOrderByTitle()).willReturn(List.of(
                new Score(band, "score 1"),
                new Score(band, "score 2")));
        given(scoreRepository.findByArrangementsIsNullOrderByTitle()).willReturn(List.of(
                new Score(band, "score 1")));
        given(instrumentRepository.findByOrderBySortOrder()).willReturn(List.of(
                new Instrument(band, "instrument 1", "i1", 1),
                new Instrument(band, "instrument 2", "i2", 2)));
    }

    @Test
    void getStatistics() {
        var statistics = statisticsService.getStatistics();
        assertThat(statistics).isNotNull();
        assertThat(statistics.getNumberOfSongs()).isEqualTo(5);
        assertThat(statistics.getNumberOfArrangements()).isEqualTo(6);
        assertThat(statistics.getNumberOfInstruments()).isEqualTo(7);
        assertThat(statistics.getNumberOfPlaylists()).isEqualTo(8);
    }

    @Test
    void writeScoresToCsv() {
        var writer = new StringWriter();
        statisticsService.writeScoresToCsv(writer);
        assertThat(writer.toString()).contains("Titel");
        assertThat(writer.toString()).hasLineCount(3);
    }

    @Test
    void writeFullScoresToCsv() {
        var writer = new StringWriter();
        statisticsService.writeFullScoresToCsv(writer);
        assertThat(writer.toString()).contains("Titel", "instrument 1");
        assertThat(writer.toString()).hasLineCount(3);
    }

    @Test
    void writeUnscannedToCsv() {
        var writer = new StringWriter();
        statisticsService.writeUnscannedToCsv(writer);
        assertThat(writer.toString()).hasLineCount(1);
    }

}