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
        this.bandRepository = bandRepository;
    }

    public Band getBand() {
        if (band == null) {
            band = bandRepository.findByName("Terrassorkestern").orElseThrow();
        }
        return band;
    }

    public void setBand(Band band) {
        log.debug("Setting band to: {} ({})", band.getName(), band.getId());
        this.band = band;
    }
}
