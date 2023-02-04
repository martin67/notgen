package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen.model.User;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(long id);
}