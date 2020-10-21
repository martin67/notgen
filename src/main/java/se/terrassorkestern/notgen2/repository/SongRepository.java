package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.Song;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Integer> {
    List<Song> findByTitle(String text);

    List<Song> findByOrderByTitle();

    List<Song> findByIdInOrderByTitle(List<Integer> id);
}
