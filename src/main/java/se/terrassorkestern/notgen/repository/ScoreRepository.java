package se.terrassorkestern.notgen.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.TopListEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoreRepository extends SearchRepository<Score, UUID> {

    Optional<Score> findByBandAndId(Band band, UUID id);

    List<Score> findByBandOrderByTitleAsc(Band band);

    List<Score> findByTitle(String text);

    List<Score> findByTitleContaining(String text);

    List<Score> findByOrderByTitle();

    List<Score> findByDefaultArrangement_ArrangementParts_InstrumentInOrderByTitleAsc(Collection<Instrument> instruments);

    List<Score> findByDefaultArrangement_ArrangementPartsInstrumentOrderByTitle(Instrument instrument);

    List<Score> findByArrangementsIsNotEmpty();

    List<Score> findByArrangementsIsNullOrderByTitle();

    @Query("SELECT s.title FROM Score s ORDER BY s.title")
    List<String> getAllTitles();

    @Query("SELECT DISTINCT s.title FROM Score s WHERE s.band = ?1 ORDER BY s.title")
    List<String> getAllTitlesByBand(Band band);

    @Query("SELECT DISTINCT s.genre FROM Score s ORDER BY s.genre")
    List<String> getAllGenres();

    @Query("SELECT DISTINCT s.genre FROM Score s WHERE s.band = ?1 ORDER BY s.genre")
    List<String> getAllGenresByBand(Band band);

    @Query("SELECT DISTINCT s.composer FROM Score s ORDER BY s.composer")
    List<String> getAllComposers();

    @Query("SELECT DISTINCT s.composer FROM Score s WHERE s.band = ?1 ORDER BY s.composer")
    List<String> getAllComposersByBand(Band band);

    @Query("SELECT DISTINCT s.author FROM Score s ORDER BY s.author")
    List<String> getAllAuthors();

    @Query("SELECT DISTINCT s.author FROM Score s WHERE s.band = ?1 ORDER BY s.author")
    List<String> getAllAuthorsByBand(Band band);

    @Query("SELECT DISTINCT s.arranger FROM Score s ORDER BY s.arranger")
    List<String> getAllArrangers();

    @Query("SELECT DISTINCT s.arranger FROM Score s WHERE s.band = ?1 ORDER BY s.arranger")
    List<String> getAllArrangersByBand(Band band);

    @Query("SELECT DISTINCT s.publisher FROM Score s ORDER BY s.publisher")
    List<String> getAllPublishers();

    @Query("SELECT DISTINCT s.publisher FROM Score s WHERE s.band = ?1 ORDER BY s.publisher")
    List<String> getAllPublishersByBand(Band band);


    // Statistics
    @Query(value = "select count(*) from arrangement", nativeQuery = true)
    long numberOfArrangements();

    @Query("SELECT " +
            " new se.terrassorkestern.notgen.model.TopListEntry(s.genre, count(s.genre)) " +
            " FROM Score s WHERE s.genre != ''GROUP BY s.genre ORDER BY COUNT(s.genre) DESC")
    List<TopListEntry> findTopGenres(Pageable pageable);

    @Query("SELECT " +
            " new se.terrassorkestern.notgen.model.TopListEntry(s.composer, count(s.composer)) " +
            " FROM Score s GROUP BY s.composer ORDER BY COUNT(s.composer) DESC")
    List<TopListEntry> findTopComposers(Pageable pageable);

    @Query("SELECT " +
            " new se.terrassorkestern.notgen.model.TopListEntry(s.arranger, count(s.arranger)) " +
            " FROM Score s WHERE s.arranger != '' GROUP BY s.arranger ORDER BY COUNT(s.arranger) DESC")
    List<TopListEntry> findTopArrangers(Pageable pageable);

    @Query("SELECT " +
            " new se.terrassorkestern.notgen.model.TopListEntry(s.author, count(s.author)) " +
            " FROM Score s WHERE s.author != '' GROUP BY s.author ORDER BY COUNT(s.author) DESC")
    List<TopListEntry> findTopAuthors(Pageable pageable);

    @Query("SELECT " +
            " new se.terrassorkestern.notgen.model.TopListEntry(s.publisher, count(s.publisher)) " +
            " FROM Score s WHERE s.publisher != '' GROUP BY s.publisher ORDER BY COUNT(s.publisher) DESC")
    List<TopListEntry> findTopPublishers(Pageable pageable);

    //@Query("SELECT SUM(sp.length) FROM ScorePart sp")
    @Query("SELECT COALESCE(SUM(ap.length), 0) FROM ArrangementPart ap")
    long numberOfPages();
}