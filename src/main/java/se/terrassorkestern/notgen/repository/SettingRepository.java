package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Setting;

import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {

    List<Setting> findByName(String text);

    Setting findFirstBy();
}