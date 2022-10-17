package se.terrassorkestern.notgen.repository;

import org.springframework.data.repository.CrudRepository;
import se.terrassorkestern.notgen.model.Organization;

import java.util.Optional;

public interface OrganizationRepository extends CrudRepository<Organization, Integer> {
    Optional<Organization> findByName(String name);
}