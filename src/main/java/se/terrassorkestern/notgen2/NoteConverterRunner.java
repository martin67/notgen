package se.terrassorkestern.notgen2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class NoteConverterRunner {

    private final Logger log = LoggerFactory.getLogger(NoteConverterController.class);

    @Autowired
    private NoteConverter noteConverter;

    void convert(List<Song> songs, boolean upload) {

        final AtomicInteger counter = new AtomicInteger(0);
        final int nofRunners = 5;

        long start = System.currentTimeMillis();

        // Split incoming songlist into x parts
        final Collection<List<Song>> partitioned = songs.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / (songs.size() / nofRunners)))
                .values();

        for (List<Song> list : partitioned) {
            log.info("i listloopen");
            noteConverter.convertP(list, upload);
        }

/*        // Kick of multiple, asynchronous lookups
        CompletableFuture<User> page1 = gitHubLookupService.findUser("PivotalSoftware");
        CompletableFuture<User> page2 = gitHubLookupService.findUser("CloudFoundry");
        CompletableFuture<User> page3 = gitHubLookupService.findUser("Spring-Projects");

        // Wait until they are all done
        CompletableFuture.allOf(page1,page2,page3).join();

        // Print results, including elapsed time
        log.info("Elapsed time: " + (System.currentTimeMillis() - start));
        logger.info("--> " + page1.get());
        logger.info("--> " + page2.get());
        logger.info("--> " + page3.get());*/
    }

}