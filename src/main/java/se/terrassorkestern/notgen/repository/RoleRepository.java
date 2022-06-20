package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen.model.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {

    Role findByName(String name);
}