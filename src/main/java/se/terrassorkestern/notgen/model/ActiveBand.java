package se.terrassorkestern.notgen.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.repository.BandRepository;

import static org.springframework.web.context.WebApplicationContext.SCOPE_SESSION;


@Slf4j
@Component
@Scope(value = SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ActiveBand {

    private final BandRepository bandRepository;
    private Band band;

    public ActiveBand(BandRepository bandRepository) {
        //this.bandRepository = bandRepository;
    log.info("Constructor");
        // First time, use the default
        //band = bandRepository.findById(1).orElseThrow();
        this.bandRepository = bandRepository;
    }

    public Band getBand() {
        if (band == null) {
            band = bandRepository.findById(1).orElseThrow();
        }
        return band;
    }

    public void setBand(Band band) {
        log.info("Setting band to: {}", band);
        this.band = band;
    }
}
