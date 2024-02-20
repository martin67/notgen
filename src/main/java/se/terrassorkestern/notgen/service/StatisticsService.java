package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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
        var statistics = new Statistics();
        statistics.setNumberOfSongs(scoreRepository.count());
        statistics.setNumberOfScannedSongs(scoreRepository.findByArrangementsIsNotEmpty().size());
        statistics.setNumberOfArrangements(scoreRepository.numberOfArrangements());
        statistics.setNumberOfScannedPages(scoreRepository.numberOfPages());
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
        var scores = scoreRepository.findByOrderByTitle();
        var csvFormat = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader(SCORE_HEADERS).build();

        try (var csvPrinter = new CSVPrinter(writer, csvFormat)) {
            for (var score : scores) {
                csvPrinter.printRecord(score.getTitle(), score.getSubTitle(), score.getGenre(), score.getYear(),
                        score.getComposer(), score.getAuthor(), score.getArranger(), score.getPublisher(),
                        score.getComment());
            }
        } catch (IOException e) {
            log.error("Error While writing CSV ", e);
        }
    }

    public void writeFullScoresToCsv(Writer writer) {
        var scores = scoreRepository.findByOrderByTitle();
        var headers = new ArrayList<>(List.of(SCORE_HEADERS));
        var allInstruments = instrumentRepository.findByOrderBySortOrder();

        for (var instrument : allInstruments) {
            headers.add(instrument.getName());
        }
        String[] header = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            header[i] = headers.get(i);
        }
        var csvFormat = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader(header).build();

        try (var csvPrinter = new CSVPrinter(writer, csvFormat)) {
            for (var score : scores) {
                var arrangement = score.getDefaultArrangement();
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
                for (var instrument : allInstruments) {
                    if (arrangement != null && arrangement.getInstruments().contains(instrument)) {
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
        var scores = scoreRepository.findByArrangementsIsNullOrderByTitle();
        try (var csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            for (var score : scores) {
                csvPrinter.printRecord(score.getTitle());
            }
        } catch (IOException e) {
            log.error("Error While writing CSV ", e);
        }
    }
}
