package se.terrassorkestern.notgen2.notelister;

import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen2.GoogleSheet;
import se.terrassorkestern.notgen2.song.ScorePart;
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.instrument.Instrument;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
// Class to create a Google Sheet with all the songs
//

@Slf4j
//@Service
class NoteLister {

    private static GoogleSheet googleSheet;

 /*   @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private SongRepository songRepository;
*/

    public NoteLister() {
        log.debug("Constructor!");

        if (googleSheet == null) {
            log.debug("Google init");
            try {
                googleSheet = new GoogleSheet();
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        } else {
            log.debug("Google sheet already initialized");
        }
    }

    void createList(List<Instrument> instruments, List<Song> songs) {
        log.info("Starting note listing");

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

        googleSheet.addRows("Repertoire!A4", repertoireRows);
        googleSheet.addRows("Sättning!A2", instrumentRows);

        List<List<Object>> dateInfo = Arrays.asList(Arrays.asList(LocalDate.now().toString()));
        googleSheet.addRows("Repertoire!E1", dateInfo);
    }

}