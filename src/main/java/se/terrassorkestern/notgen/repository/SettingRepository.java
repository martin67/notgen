package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    Optional<Setting> findByIdAndBand(UUID id, Band band);

    Setting findFirstBy();

    Setting findFirstByBand(Band band);
}