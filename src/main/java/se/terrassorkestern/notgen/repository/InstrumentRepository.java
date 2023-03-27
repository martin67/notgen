package se.terrassorkestern.notgen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Instrument;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

    List<Instrument> findByBandOrderBySortOrder(Band band);

    List<Instrument> findByName(String text);

    List<Instrument> findByNameContaining(String text);

    List<Instrument> findByOrderBySortOrder();

    Optional<Instrument> findFirstByBand(Band band);

    Optional<Instrument> findByBandAndId(Band band, int id);

}