package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Transactional
public class ScoreUpdater {

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Score updater");
    }

}
