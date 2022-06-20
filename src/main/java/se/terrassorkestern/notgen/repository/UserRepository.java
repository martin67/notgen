package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);

}