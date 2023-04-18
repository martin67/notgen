package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Playlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    List<Playlist> findByName(String text);

    List<Playlist> findAllByOrderByDateDesc();

    List<Playlist> findByBandOrderByDateDesc(Band band);

    Optional<Playlist> findByBandAndId(Band band, UUID id);
}