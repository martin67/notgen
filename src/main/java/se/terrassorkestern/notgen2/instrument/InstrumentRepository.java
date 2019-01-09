package se.terrassorkestern.notgen2.instrument;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

  // custom query to search to blog post by title or content
  List<Instrument> findByName(String text);
  
  List<Instrument> findByOrderByStandardDescSortOrder();
  
  List<Instrument> findByStandardIsTrueOrderBySortOrder();
}