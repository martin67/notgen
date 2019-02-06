package se.terrassorkestern.notgen2.song;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Integer> {

    // custom query to search to blog post by title or content
    List<Song> findByTitle(String text);

    List<Song> findByOrderByTitle();

    List<Song> findByIdInOrderByTitle(List<Integer> id);

    @Query("SELECT s.title FROM Song s ORDER BY s.title")
    List<String> getAllTitles();

    @Query("SELECT DISTINCT s.genre FROM Song s ORDER BY s.genre")
    List<String> getAllGenres();

    @Query("SELECT DISTINCT s.composer FROM Song s ORDER BY s.composer")
    List<String> getAllComposers();

    @Query("SELECT DISTINCT s.author FROM Song s ORDER BY s.author")
    List<String> getAllAuthors();

    @Query("SELECT DISTINCT s.arranger FROM Song s ORDER BY s.arranger")
    List<String> getAllArrangers();

    @Query("SELECT DISTINCT s.publisher FROM Song s ORDER BY s.publisher")
    List<String> getAllPublishers();

}