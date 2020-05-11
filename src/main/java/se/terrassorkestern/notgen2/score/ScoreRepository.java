package se.terrassorkestern.notgen2.score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    // custom query to search to blog post by title or content
    List<Score> findByTitle(String text);

    List<Score> findByOrderByTitle();

    List<Score> findByIdInOrderByTitle(List<Integer> id);

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

}