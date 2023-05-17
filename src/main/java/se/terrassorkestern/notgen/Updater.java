package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Link;
import se.terrassorkestern.notgen.repository.LinkRepository;

@Slf4j
@Component
@Transactional
public class Updater {

    private final LinkRepository linkRepository;

    public Updater(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Updater start");
        for (Link link : linkRepository.findAll()) {
            if (link.getUri() != null) {
                link.setUri2(link.getUri().toString());
                linkRepository.save(link);
            } else {
                log.warn("No URL for link {}, score {}", link, link.getScore());
            }
        }
        log.info("********* Updater end");
    }
}
