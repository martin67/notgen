package se.terrassorkestern.notgen2.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen2.model.Privilege;

public interface PrivilegeRepository extends CrudRepository<Privilege, Long> {

    Privilege findByName(String name);
}