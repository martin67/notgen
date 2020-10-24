package se.terrassorkestern.notgen2.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen2.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);

}