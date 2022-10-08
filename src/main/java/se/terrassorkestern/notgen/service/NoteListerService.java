package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import se.terrassorkestern.notgen.repository.ScoreRepository;

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
public class NoteListerService {

    private final InstrumentRepository instrumentRepository;
    private final ScoreRepository scoreRepository;


    public NoteListerService(InstrumentRepository instrumentRepository, ScoreRepository scoreRepository) {
        this.instrumentRepository = instrumentRepository;
        this.scoreRepository = scoreRepository;
    }

    public void createList() {
        log.info("Starting note listing");

        List<Instrument> instruments = instrumentRepository.findAll();
        List<Score> scores = scoreRepository.findByOrderByTitle();

        List<List<Object>> repertoireRows = new ArrayList<>();
        List<List<Object>> instrumentRows = new ArrayList<>();

        for (Score score : scores) {

            // Första fliken
            repertoireRows.add(
                    Arrays.asList(
                            score.getTitle(),
                            score.getSubTitle(),
                            score.getGenre(),
                            score.getAuthor(),
                            score.getComposer(),
                            score.getArranger(),
                            score.getYear()
                    )
            );

            // Andra fliken
            //instrumentRows.add()
            List<Object> instrumentRow = new ArrayList<>();
            instrumentRow.add(score.getTitle());
            instrumentRow.add(score.getGenre());

            // Kolla för varje instrument i ordning om det finns i låten
            for (Instrument instrument : instruments) {
                boolean hit = false;
                for (ScorePart scorePart : score.getScoreParts()) {
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

        //googleSheetService.addRows("Repertoire!A4", repertoireRows);
        //googleSheetService.addRows("Sättning!A2", instrumentRows);

        List<List<Object>> dateInfo = Collections.singletonList(Collections.singletonList(LocalDate.now().toString()));
        //googleSheetService.addRows("Repertoire!E1", dateInfo);
    }

}