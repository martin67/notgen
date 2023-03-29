package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.ArrangementPart;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
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
        log.info("********* Score updater");

        for (Score score : scoreRepository.findAll()) {
            if (score.getArrangements().isEmpty()) {
                log.info("Fixing arrangements for {} ({})", score.getTitle(), score.getId());
                Arrangement arrangement = new Arrangement();
                arrangement.setArranger(score.getArranger());
                for (ScorePart scorePart : score.getScoreParts()) {
                    ArrangementPart arrangementPart = new ArrangementPart(scorePart);
                    arrangement.addArrangementPart(arrangementPart);
                }
                score.addArrangement(arrangement);
                score.setDefaultArrangement(arrangement);
                scoreRepository.save(score);
            }
        }
    }
}
