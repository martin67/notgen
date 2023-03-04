package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);

    Optional<User> findByBandsContainingAndId(Band band, long id);

    List<User> findByBandsContaining(Band band);
}