package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class StatisticsService {
    private static final int TOPLIST_COUNT = 10;
    private static final String[] SCORE_HEADERS = {"Titel", "Subtitel", "Genre", "År", "Kompositör", "Författare",
            "Arrangör", "Förlag", "Kommentar"};
    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistRepository playlistRepository;


    public StatisticsService(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                             PlaylistRepository playlistRepository) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistRepository = playlistRepository;
    }

    public Statistics getStatistics() {
        Statistics statistics = new Statistics();

        StopWatch stopWatch = new StopWatch("admin statistics");

        stopWatch.start("setNumberOfSongs");
        statistics.setNumberOfSongs(scoreRepository.count());
        stopWatch.stop();
        stopWatch.start("setNumberOfScannedSongs");
        statistics.setNumberOfScannedSongs(scoreRepository.countByScannedIsTrue());
        stopWatch.stop();

        stopWatch.start("setNumberOfScannedPages");
        statistics.setNumberOfScannedPages(scoreRepository.numberOfPages());
        stopWatch.stop();

        stopWatch.start("setNumberOfInstruments");
        statistics.setNumberOfInstruments(instrumentRepository.count());
        stopWatch.stop();
        stopWatch.start("setNumberOfPlaylists");
        statistics.setNumberOfPlaylists(playlistRepository.count());
        stopWatch.stop();

        stopWatch.start("setTop");
        statistics.setTopGenres(scoreRepository.findTopGenres(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopComposers(scoreRepository.findTopComposers(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopArrangers(scoreRepository.findTopArrangers(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopAuthors(scoreRepository.findTopAuthors(PageRequest.of(0, TOPLIST_COUNT)));
        statistics.setTopPublishers(scoreRepository.findTopPublishers(PageRequest.of(0, TOPLIST_COUNT)));
        stopWatch.stop();

        log.info(stopWatch.prettyPrint());
        log.info("Total time: {} s", stopWatch.getTotalTimeSeconds());
        log.info("Total time: {} ms", stopWatch.getTotalTimeMillis());
        return statistics;
    }

    public void writeScoresToCsv(Writer writer) {
        List<Score> scores = scoreRepository.findByOrderByTitle();
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader(SCORE_HEADERS).build();

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

    public void writeFullScoresToCsv(Writer writer) {
        List<Score> scores = scoreRepository.findByOrderByTitle();
        List<String> headers = new ArrayList<>(List.of(SCORE_HEADERS));
        List<Instrument> allInstruments = instrumentRepository.findByOrderBySortOrder();

        for (Instrument instrument : allInstruments) {
            headers.add(instrument.getName());
        }
        String[] header = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            header[i] = headers.get(i);
        }
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader(header).build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            for (Score score : scores) {
                List<String> values = new ArrayList<>();
                values.add(score.getTitle());
                values.add(score.getSubTitle());
                values.add(score.getGenre());
                values.add(String.valueOf(score.getYear()));
                values.add(score.getComposer());
                values.add(score.getAuthor());
                values.add(score.getArranger());
                values.add(score.getPublisher());
                values.add(score.getComment());
                for (Instrument instrument : allInstruments) {
                    if (score.getInstruments().contains(instrument)) {
                        values.add("X");
                    } else {
                        values.add("");
                    }
                }
                csvPrinter.printRecord(values);
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
