package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Setting;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {

    List<Setting> findByName(String text);

    List<Setting> findByBand(Band band);

    Optional<Setting> findByIdAndBand(int id, Band band);

    Setting findFirstBy();
}