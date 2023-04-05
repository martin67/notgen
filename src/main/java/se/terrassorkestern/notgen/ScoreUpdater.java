package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.NgFileRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.StorageService;

import java.io.IOException;

@Slf4j
@Component
@Transactional
public class ScoreUpdater {

    private final ScoreRepository scoreRepository;
    private final StorageService storageService;
    private final NgFileRepository fileRepository;

    public ScoreUpdater(ScoreRepository scoreRepository, StorageService storageService, NgFileRepository fileRepository) {
        this.scoreRepository = scoreRepository;
        this.storageService = storageService;
        this.fileRepository = fileRepository;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException {
        log.info("********* Score updater");
        if (true)
            return;
        for (Score score : scoreRepository.findAll()) {
            if (score.getArrangements().isEmpty() && score.getScanned()) {
                log.info("Fixing arrangements for {} ({})", score.getTitle(), score.getId());
                Arrangement arrangement = new Arrangement();
                arrangement.setArranger(score.getArranger());
                arrangement.setName("Original");
                for (ScorePart scorePart : score.getScoreParts()) {
                    ArrangementPart arrangementPart = new ArrangementPart(scorePart);
                    arrangement.addArrangementPart(arrangementPart);
                }
                score.addArrangement(arrangement);
                score.setDefaultArrangement(arrangement);

                // Only add default arrangement
                NgFile file = new NgFile();
                // Save to get an updated id.
                fileRepository.save(file);
                String filename = storageService.renameScore(score, String.valueOf(file.getId())).getFileName().toString();
                file.setFilename(filename);
                file.setOriginalFilename(score.getFilename());
                file.setType(NgFileType.Arrangement);
                arrangement.setFile(file);
                scoreRepository.save(score);
            }
        }
    }
}
