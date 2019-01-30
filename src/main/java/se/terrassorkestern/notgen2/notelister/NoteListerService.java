package se.terrassorkestern.notgen2.notelister;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.song.SongRepository;

@Slf4j
@Service
public class NoteListerService {

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;


    NoteListerService(MeterRegistry meterRegistry) {
        log.debug("Constructor");
        MeterRegistry meterRegistry1 = meterRegistry;
    }


    public void createList() {
        log.debug("createList");

        NoteLister noteLister = new NoteLister();
        noteLister.createList(instrumentRepository.findByOrderByStandardDescSortOrder(),
                songRepository.findByOrderByTitle());
    }

}
