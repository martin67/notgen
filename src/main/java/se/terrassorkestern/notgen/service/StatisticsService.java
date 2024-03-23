package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        writeFullScoresToCsv(writer, scores);
    }

    public void writeFullScoresToCsv(Writer writer, Playlist playlist) {
        List<Score> scores = new ArrayList<>();
        for (var playlistEntry : playlist.getPlaylistEntries()) {
            var scoresFound = scoreRepository.findByTitle(playlistEntry.getText());
            if (scoresFound != null && !scoresFound.isEmpty()) {
                if (scoresFound.size() > 1) {
                    log.warn("Multiple scores for playlist entry {}", playlistEntry.getText());
                }
                scores.add(scoresFound.get(0));
            }
        }
        writeFullScoresToCsv(writer, scores);
    }

    private void writeFullScoresToCsv(Writer writer, List<Score> scores) {
        var headers = new ArrayList<>(List.of(SCORE_HEADERS));

        // Needed for hibernate, otherwise the scores.stream will return empty instruments...
        instrumentRepository.findAll();

        var allUsedInstruments = scores.stream()
                .map(Score::getDefaultArrangement)
                .flatMap(arr -> arr.getInstruments().stream())
                .collect(Collectors.toSet())
                .stream().sorted().toList();

        for (var instrument : allUsedInstruments) {
            headers.add(instrument.getName());
        }
        String[] header = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            header[i] = headers.get(i);
        }
        var csvFormat = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader(header).build();

        try (var csvPrinter = new CSVPrinter(writer, csvFormat)) {
            for (var score : scores) {
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

                var arrangement = score.getDefaultArrangement();
                if (arrangement != null) {
                    for (var instrument : allUsedInstruments) {
                        var ap = arrangement.getArrangementPart(instrument);
                        if (ap.isPresent()) {
                            values.add("X" + (ap.get().getComment() == null || ap.get().getComment().isEmpty() ? "" : " (" + ap.get().getComment() + ")"));
                        } else {
                            values.add("");
                        }
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
