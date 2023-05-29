package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.ConfigurationKey;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfigurationKeyRepository extends JpaRepository<ConfigurationKey, UUID> {
    Optional<ConfigurationKey> findByToken(String token);
}