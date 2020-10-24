package se.terrassorkestern.notgen2.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen2.model.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {

    Role findByName(String name);
}