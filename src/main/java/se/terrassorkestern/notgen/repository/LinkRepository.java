package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Link;

import java.util.List;
import java.util.UUID;

@Repository
public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByOrderByName();
}