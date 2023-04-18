package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.NgFile;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NgFileRepository extends JpaRepository<NgFile, UUID> {
    Optional<NgFile> findByName(String name);
}