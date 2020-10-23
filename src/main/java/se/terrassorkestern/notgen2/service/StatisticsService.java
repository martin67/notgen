package se.terrassorkestern.notgen2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.repository.ImageDataRepository;
import se.terrassorkestern.notgen2.repository.InstrumentRepository;
import se.terrassorkestern.notgen2.repository.PlaylistRepository;
import se.terrassorkestern.notgen2.repository.ScoreRepository;
import se.terrassorkestern.notgen2.model.Statistics;

@Service
public class StatisticsService {
    static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistRepository playlistRepository;
    private final ImageDataRepository imageDataRepository;

    public StatisticsService(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                             PlaylistRepository playlistRepository, ImageDataRepository imageDataRepository) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistRepository = playlistRepository;
        this.imageDataRepository = imageDataRepository;
    }

    public Statistics getStatistics() {
        Statistics statistics = new Statistics();
            statistics.setNumberOfSongs(scoreRepository.count());
            statistics.setNumberOfScannedSongs(scoreRepository.countByScannedIsFalse());
            statistics.setNumberOfScannedPages(imageDataRepository.count());
            statistics.setNumberOfBytes(imageDataRepository.sumSize());
            statistics.setNumberOfBytes(instrumentRepository.sumSortOrder());
            statistics.setNumberOfInstruments(instrumentRepository.count());
            statistics.setNumberOfPlaylists(playlistRepository.count());
            //statistics.setTopGenres(scoreRepository.getTop5Genres());

        return statistics;
    }
}
