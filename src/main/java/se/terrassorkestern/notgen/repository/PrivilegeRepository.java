package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Privilege;

import java.util.UUID;

@Repository
public interface PrivilegeRepository extends CrudRepository<Privilege, UUID> {

    Privilege findByName(String name);
}