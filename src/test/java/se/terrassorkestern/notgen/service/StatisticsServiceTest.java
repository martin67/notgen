package se.terrassorkestern.notgen.service;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.terrassorkestern.notgen.model.*;
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
        var instrument1 = new Instrument(band, "instrument 1", "i1", 1);
        var instrument2 = new Instrument(band, "instrument 2", "i2", 2);
        var instrument3 = new Instrument(band, "instrument 3", "i3", 3);

        Score score1 = new Score(band, "score 1");
        Arrangement arr1 = new Arrangement("arr 1");
        arr1.addArrangementPart(new ArrangementPart(arr1, instrument1));
        score1.setDefaultArrangement(arr1);

        Score score2 = new Score(band, "score 2");

        Score score3 = new Score(band, "score 3");
        Arrangement arr3 = new Arrangement("arr 3");
        arr3.addArrangementPart(new ArrangementPart(arr3, instrument3));
        score3.setDefaultArrangement(arr3);

        given(scoreRepository.findByOrderByTitle()).willReturn(List.of(score1, score2, score3));
        given(scoreRepository.findByArrangementsIsNullOrderByTitle()).willReturn(List.of(score2));
        given(instrumentRepository.findByOrderBySortOrder()).willReturn(List.of(instrument1, instrument2, instrument3));
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
        assertThat(writer.toString()).hasLineCount(4);
    }

    @Test
    void writeFullScoresToCsv() {
        var writer = new StringWriter();
        statisticsService.writeFullScoresToCsv(writer);
        assertThat(writer.toString()).contains("Titel", "instrument 1");
        assertThat(writer.toString()).hasLineCount(4);
    }

    @Test
    void writeUnscannedToCsv() {
        var writer = new StringWriter();
        statisticsService.writeUnscannedToCsv(writer);
        assertThat(writer.toString()).hasLineCount(1);
    }

}