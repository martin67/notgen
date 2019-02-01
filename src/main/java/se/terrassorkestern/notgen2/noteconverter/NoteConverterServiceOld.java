package se.terrassorkestern.notgen2.noteconverter;

// TODO remove file

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.List;

@Slf4j
@Service
public class NoteConverterServiceOld {

  private MeterRegistry meterRegistry;
  @Autowired
  private SongRepository songRepository;


  NoteConverterServiceOld(MeterRegistry meterRegistry) {
    log.debug("Constructor");
    this.meterRegistry = meterRegistry;
  }


  public void convert(List<Integer> selectedSongs, boolean allSongs, boolean upload) {
    log.debug("convert");

    //NoteConverterService noteConverterService = new NoteConverterService();
    NoteConverterStats stats = new NoteConverterStats();

    // Starta konvertering!
    if (allSongs) {
      //stats = noteConverterService.convert(songRepository, songRepository.findByOrderByTitle(), upload);
    } else {
      //stats = noteConverterService.convert(songRepository, songRepository.findByIdInOrderByTitle(selectedSongs), upload);
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
