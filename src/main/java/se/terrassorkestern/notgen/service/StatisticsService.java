package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.ImagedataRepository;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

@Slf4j
@Service
public class StatisticsService {
    static final int TOPLIST_COUNT = 5;
    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistRepository playlistRepository;
    private final ImagedataRepository imagedataRepository;


    public StatisticsService(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                             PlaylistRepository playlistRepository, ImagedataRepository imagedataRepository) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistRepository = playlistRepository;
        this.imagedataRepository = imagedataRepository;
    }

    public Statistics getStatistics() {
        Statistics statistics = new Statistics();

        statistics.setNumberOfSongs(scoreRepository.count());
        statistics.setNumberOfScannedSongs(scoreRepository.countByScannedIsTrue());
        statistics.setNumberOfScannedPages(imagedataRepository.count());
//        statistics.setNumberOfBytes(imagedataRepository.sumSize());
        statistics.setNumberOfInstruments(instrumentRepository.count());
        statistics.setNumberOfPlaylists(playlistRepository.count());

        statistics.setTopGenres(scoreRepository.findTopGenres(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopComposers(scoreRepository.findTopComposers(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopArrangers(scoreRepository.findTopArrangers(PageRequest.of(0, TOPLIST_COUNT)));

        return statistics;
    }
}
