package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ScoreRepository;

@Slf4j
@Component
@Transactional
public class ScoreUpdater {

    private final ScoreRepository scoreRepository;

    public ScoreUpdater(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Score updater starting");
        for (Score score : scoreRepository.findAll()) {
            if (score.getArrangements() != null) {
                for (Arrangement arrangement : score.getArrangements()) {
                    arrangement.setCover(score.getCover());
                    arrangement.setArchiveLocation(score.getArchiveLocation());
                }
                scoreRepository.save(score);
            }
        }
        log.info("********* Score updater completed");
    }

}
