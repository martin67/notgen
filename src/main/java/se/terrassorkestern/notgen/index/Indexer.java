package se.terrassorkestern.notgen.index;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.exceptions.IndexException;

@Slf4j
@Transactional
@Component
public class Indexer {

    private final EntityManager entityManager;
    private static final int THREAD_NUMBER = 4;

    public Indexer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void indexPersistedData(String indexClassName) throws IndexException {
        try {
            var searchSession = Search.session(entityManager);

            Class<?> classToIndex = Class.forName(indexClassName);
            var indexer =
                    searchSession
                            .massIndexer(classToIndex)
                            .threadsToLoadObjects(THREAD_NUMBER);
            indexer.startAndWait();
        } catch (ClassNotFoundException e) {
            throw new IndexException("Invalid class " + indexClassName, e);
        } catch (InterruptedException e) {
            log.error("Index Interrupted", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }
}