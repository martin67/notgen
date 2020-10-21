package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.Playlist;

import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

    List<Playlist> findByName(String text);

    List<Playlist> findAllByOrderByDateDesc();
}