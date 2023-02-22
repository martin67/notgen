package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;

import java.util.Optional;

@Repository
public interface BandRepository extends JpaRepository<Band, Integer> {
    Optional<Band> findByName(String name);
}