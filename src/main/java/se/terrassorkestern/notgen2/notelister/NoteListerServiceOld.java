package se.terrassorkestern.notgen2.notelister;


// TODO remove

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.song.SongRepository;

@Slf4j
@Service
public class NoteListerServiceOld {

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;


    NoteListerServiceOld(MeterRegistry meterRegistry) {
        log.debug("Constructor");
        MeterRegistry meterRegistry1 = meterRegistry;
    }


    public void createList() {
        log.debug("createList");

        NoteListerService noteLister = new NoteListerService();
//        noteLister.createList(instrumentRepository.findByOrderByStandardDescSortOrder(),
//                songRepository.findByOrderByTitle());
    }

}
