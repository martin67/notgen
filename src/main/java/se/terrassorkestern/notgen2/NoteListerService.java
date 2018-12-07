package se.terrassorkestern.notgen2;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NoteListerService {

    private MeterRegistry meterRegistry;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;


    NoteListerService(MeterRegistry meterRegistry) {
        log.debug("Constructor");
        this.meterRegistry = meterRegistry;
    }


    public void createList() {
        log.debug("createList");

        NoteLister noteLister = new NoteLister();
        noteLister.createList(instrumentRepository.findByOrderByStandardDescSortOrder(),
                songRepository.findByOrderByTitle());
    }

}
