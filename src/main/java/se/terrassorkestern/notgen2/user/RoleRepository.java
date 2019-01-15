package se.terrassorkestern.notgen2.user;

import org.springframework.data.repository.CrudRepository;

public interface RoleRepository extends CrudRepository<Role, Long> {

  Role findByName(String name);
}