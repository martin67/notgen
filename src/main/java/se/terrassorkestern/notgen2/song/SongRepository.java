package se.terrassorkestern.notgen2.song;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Integer> {

    // custom query to search to blog post by title or content
    List<Song> findByTitle(String text);
    List<Song> findByOrderByTitle();
    List<Song> findByIdInOrderByTitle(List<Integer> id);
}