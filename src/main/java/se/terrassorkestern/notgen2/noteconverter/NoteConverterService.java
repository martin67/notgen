package se.terrassorkestern.notgen2.noteconverter;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.List;

@Slf4j
@Service
public class NoteConverterService {

  private MeterRegistry meterRegistry;
  @Autowired
  private SongRepository songRepository;


  NoteConverterService(MeterRegistry meterRegistry) {
    log.debug("Constructor");
    this.meterRegistry = meterRegistry;
  }


  public void convert(List<Integer> selectedSongs, boolean allSongs, boolean upload) {
    log.debug("convert");

    NoteConverter noteConverter = new NoteConverter();
    NoteConverterStats stats;

    // Starta konvertering!
    if (allSongs) {
      stats = noteConverter.convert(songRepository.findByOrderByTitle(), upload);
    } else {
      stats = noteConverter.convert(songRepository.findByIdInOrderByTitle(selectedSongs), upload);
    }

    meterRegistry.counter("notgen.stats", "Hej", "Songs processed").increment(stats.getNumberOfSongs());
    meterRegistry.counter("notgen.stats", "Hej", "Files extracted").increment(stats.getNumberOfSrcImg());
    meterRegistry.counter("notgen.stats", "Hej", "Images processed").increment(stats.getNumberOfImgProcess());
    meterRegistry.counter("notgen.stats", "Hej", "PDFs created").increment(stats.getNumberOfPdf());
    meterRegistry.counter("notgen.stats", "Hej", "Covers created").increment(stats.getNumberOfCovers());
    meterRegistry.counter("notgen.stats", "Hej", "Lyrics OCR").increment(stats.getNumberOfOcr());
    meterRegistry.counter("notgen.stats", "Hej", "Bytes uploaded").increment(stats.getNumberOfBytes());
  }

}
