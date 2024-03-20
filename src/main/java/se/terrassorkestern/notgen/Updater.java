package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScoreType;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.util.List;

@Slf4j
@Component
@Transactional
public class Updater {
    final
    ScoreRepository scoreRepository;

    public Updater(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Updater start");
        var st = List.of(ScoreType.values());
        log.info("Score types: {}", st.size());
        log.info("Syncing arranger and publisher");
        for (Score score : scoreRepository.findAll()) {
            if (score.getArrangements().isEmpty()) {
                log.info("Score {} har inga arr", score);
            } else  {
                for (Arrangement arrangement : score.getArrangements()) {
                    arrangement.setArranger(score.getArranger());
                    arrangement.setPublisher(score.getPublisher());
                }
                scoreRepository.save(score);
            }
        }
        log.info("********* Updater end");
    }
}
