package se.terrassorkestern.notgen2.instrument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Integer> {

    List<Setting> findByName(String text);

}