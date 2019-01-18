package se.terrassorkestern.notgen2.playlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

  // custom query to search to blog post by title or content
  List<Playlist> findByName(String text);
  
  List<Playlist> findAllByOrderByDateDesc();
}