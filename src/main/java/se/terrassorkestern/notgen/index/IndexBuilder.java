package se.terrassorkestern.notgen.index;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Slf4j
@Configuration
public class IndexBuilder implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.jpa.properties.hibernate.search.enabled:true}")
    private String jpaSearchEnabled;

    private final EntityManager entityManager;


    public IndexBuilder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    @Transactional
    public void onApplicationEvent(@Nullable ApplicationReadyEvent event) {

        if (Boolean.parseBoolean(jpaSearchEnabled)) {
            log.info("Started Initializing Indexes");
            var searchSession = Search.session(entityManager);

            var indexer = searchSession.massIndexer().idFetchSize(150).batchSizeToLoadObjects(25)
                    .threadsToLoadObjects(12);
            try {
                indexer.startAndWait();
            } catch (InterruptedException e) {
                log.warn("Failed to load data from database");
                Thread.currentThread().interrupt();
            }
            log.info("Completed Indexing");
        }
    }
}