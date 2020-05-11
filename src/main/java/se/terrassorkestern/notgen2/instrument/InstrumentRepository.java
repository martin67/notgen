package se.terrassorkestern.notgen2.instrument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

    List<Instrument> findByName(String text);

//    List<Instrument> findByOrderByStandardDescSortOrder();
//
//    List<Instrument> findByStandardIsTrueOrderBySortOrder();
}