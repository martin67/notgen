package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen.model.Privilege;

public interface PrivilegeRepository extends CrudRepository<Privilege, Long> {

    Privilege findByName(String name);
}