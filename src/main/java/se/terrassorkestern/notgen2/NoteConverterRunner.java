package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NoteConverterRunner {

    @Autowired
    private NoteConverter noteConverter1;
    @Autowired
    private NoteConverter noteConverter2;
    @Autowired
    private NoteConverter noteConverter3;

    void convert(List<Song> songs, boolean upload) {

        final AtomicInteger counter = new AtomicInteger(0);
        final int nofRunners = 5;

        long start = System.currentTimeMillis();

        CompletableFuture<Integer> nc1 = noteConverter1.convertP(songs.subList(0, 100),upload);
        CompletableFuture<Integer> nc2 = noteConverter2.convertP(songs.subList(101, 200),upload);
        CompletableFuture<Integer> nc3 = noteConverter3.convertP(songs.subList(201, 300),upload);

        // Split incoming songlist into x parts
        final Collection<List<Song>> partitioned = songs.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / (songs.size() / nofRunners)))
                .values();

        for (List<Song> list : partitioned) {
            log.info("i listloopen");
            // Går inte, bara en instans av NoteConverter. Måste vara en per tråd som skapas
            //noteConverter.convertP(list, upload);
        }

        CompletableFuture.allOf(nc1,nc2,nc3).join();

        log.info("Elapsed time: " + (System.currentTimeMillis() - start));
        try {
            log.info("--> " + nc1.get());
            log.info("--> " + nc2.get());
            log.info("--> " + nc3.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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