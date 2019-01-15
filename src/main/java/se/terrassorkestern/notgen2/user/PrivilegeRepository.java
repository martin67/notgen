package se.terrassorkestern.notgen2.user;

import org.springframework.data.repository.CrudRepository;

public interface PrivilegeRepository extends CrudRepository<Privilege, Long> {
  
  Privilege findByName(String name);
}