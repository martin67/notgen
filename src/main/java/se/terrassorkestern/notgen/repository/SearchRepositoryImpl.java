package se.terrassorkestern.notgen.repository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.List;

@Transactional
public class SearchRepositoryImpl<T, I extends Serializable> extends SimpleJpaRepository<T, I>
        implements SearchRepository<T, I> {

    private final EntityManager entityManager;


    public SearchRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    public SearchRepositoryImpl(
            JpaEntityInformation<T, I> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public List<T> searchBy(String text, int limit, String... fields) {

        SearchResult<T> result = getSearchResult(text, limit, fields);

        return result.hits();
    }

    private SearchResult<T> getSearchResult(String text, int limit, String[] fields) {
        var searchSession = Search.session(entityManager);

        return searchSession
                .search(getDomainClass())
                .where(f -> f.match()
                        .fields(fields)
                        .matching(text)
                        .fuzzy(0))
                .fetch(limit);
    }
}