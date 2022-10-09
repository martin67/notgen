package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.ImagedataRepository;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Slf4j
@Service
public class StatisticsService {
    private static final int TOPLIST_COUNT = 10;
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
        // Todo: solve this directly in JPA instead
        int scannedPages = 0;
        for (Score score : scoreRepository.findAll()) {
            for (ScorePart scorePart : score.getScoreParts()) {
                scannedPages += scorePart.getLength();
            }
        }
        statistics.setNumberOfScannedPages(scannedPages);
        statistics.setNumberOfInstruments(instrumentRepository.count());
        statistics.setNumberOfPlaylists(playlistRepository.count());

        statistics.setTopGenres(scoreRepository.findTopGenres(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopComposers(scoreRepository.findTopComposers(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopArrangers(scoreRepository.findTopArrangers(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopAuthors(scoreRepository.findTopAuthors(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopPublishers(scoreRepository.findTopPublishers(PageRequest.of(0, TOPLIST_COUNT)));

        return statistics;
    }

    public void writeScoresToCsv(Writer writer) {
        List<Score> scores = scoreRepository.findByOrderByTitle();
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';')
                .setHeader("Titel", "Subtitel", "Genre", "År", "Kompositör", "Författare", "Arrangör", "Förlag", "Kommentar").build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            for (Score score : scores) {
                csvPrinter.printRecord(score.getTitle(), score.getSubTitle(), score.getGenre(), score.getYear(),
                        score.getComposer(), score.getAuthor(), score.getArranger(), score.getPublisher(),
                        score.getComment());
            }
        } catch (IOException e) {
            log.error("Error While writing CSV ", e);
        }
    }

    public void writeUnscannedToCsv(Writer writer) {
        List<Score> scores = scoreRepository.findByScannedFalseOrderByTitle();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            for (Score score : scores) {
                csvPrinter.printRecord(score.getTitle());
            }
        } catch (IOException e) {
            log.error("Error While writing CSV ", e);
        }
    }
}
