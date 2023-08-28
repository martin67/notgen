package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Setting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, UUID> {

    List<Setting> findByName(String text);

    List<Setting> findByBand(Band band);

    @Query("select s from Setting s join fetch s.instruments i where s.band = ?1 and s.id = ?2")
    Optional<Setting> findByBandAndId(Band band, UUID id);

    Optional<Setting> findFirstByBand(Band band);
}