package se.terrassorkestern.notgen2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen2.model.Instrument;

import java.util.List;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

    List<Instrument> findByName(String text);

//    List<Instrument> findByOrderByStandardDescSortOrder();
//
//    List<Instrument> findByStandardIsTrueOrderBySortOrder();

    @Query("SELECT sum(sortOrder) FROM Instrument")
    Long sumSortOrder();
}