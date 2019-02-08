package se.terrassorkestern.notgen2.notelister;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.google.GoogleSheetService;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.song.ScorePart;
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//
// Class to create a Google Sheet with all the songs
//

@Slf4j
@Service
@AllArgsConstructor
public class NoteListerService {

    private final @NonNull InstrumentRepository instrumentRepository;
    private final @NonNull SongRepository songRepository;
    private final @NonNull GoogleSheetService googleSheetService;


    void createList() {
        log.info("Starting note listing");

        List<Instrument> instruments = instrumentRepository.findByOrderByStandardDescSortOrder();
        List<Song> songs = songRepository.findByOrderByTitle();

        List<List<Object>> repertoireRows = new ArrayList<>();
        List<List<Object>> instrumentRows = new ArrayList<>();

        for (Song song : songs) {

            // Första fliken
            repertoireRows.add(
                    Arrays.asList(
                            song.getTitle(),
                            song.getSubtitle(),
                            song.getGenre(),
                            song.getAuthor(),
                            song.getComposer(),
                            song.getArranger(),
                            song.getYear()
                    )
            );

            // Andra fliken
            //instrumentRows.add()
            List<Object> instrumentRow = new ArrayList<>();
            instrumentRow.add(song.getTitle());
            instrumentRow.add(song.getGenre());

            // Kolla för varje instrument i ordning om det finns i låten
            for (Instrument instrument : instruments) {
                boolean hit = false;
                for (ScorePart scorePart : song.getScoreParts()) {
                    if (scorePart.getInstrument().getId() == instrument.getId()) {
                        instrumentRow.add(scorePart.getLength());
                        hit = true;
                    }
                }
                if (!hit) {
                    instrumentRow.add("");
                }
            }
            instrumentRows.add(instrumentRow);
        }


        log.info("Writing to Google Sheet");

        googleSheetService.addRows("Repertoire!A4", repertoireRows);
        googleSheetService.addRows("Sättning!A2", instrumentRows);

        List<List<Object>> dateInfo = Collections.singletonList(Collections.singletonList(LocalDate.now().toString()));
        googleSheetService.addRows("Repertoire!E1", dateInfo);
    }

}