package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.Score;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    // custom query to search to blog post by title or content
    List<Score> findByTitle(String text);

    List<Score> findByTitleContaining(String text);

    List<Score> findByOrderByTitle();

    List<Score> findByIdInOrderByTitle(List<Integer> id);

    List<Score> findByIdGreaterThan(int id);

    @Query("SELECT s.title FROM Score s ORDER BY s.title")
    List<String> getAllTitles();

    @Query("SELECT DISTINCT s.genre FROM Score s ORDER BY s.genre")
    List<String> getAllGenres();

    @Query("SELECT DISTINCT s.composer FROM Score s ORDER BY s.composer")
    List<String> getAllComposers();

    @Query("SELECT DISTINCT s.author FROM Score s ORDER BY s.author")
    List<String> getAllAuthors();

    @Query("SELECT DISTINCT s.arranger FROM Score s ORDER BY s.arranger")
    List<String> getAllArrangers();

    @Query("SELECT DISTINCT s.publisher FROM Score s ORDER BY s.publisher")
    List<String> getAllPublishers();


    // Statistics
    long count();

    long countByScannedIsFalse();

    //select genre, count(genre) from score group by genre order by count(genre) desc limit 5;
    @Query("SELECT s.genre, COUNT(s.genre) FROM Score s GROUP BY s.genre ORDER BY COUNT(s.genre) DESC")
    List<List<String>> getTop5Genres();
}