package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.ScoreType;

import java.util.List;

@Slf4j
@Component
@Transactional
public class Updater {

    public Updater() {
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Updater start");
        var st = List.of(ScoreType.values());
        log.info("st: {}", st.size());
        log.info("********* Updater end");
    }
}
