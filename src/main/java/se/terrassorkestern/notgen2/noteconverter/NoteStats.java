package se.terrassorkestern.notgen2.noteconverter;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.playlist.PlaylistRepository;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.HashMap;
import java.util.Map;

@Component
public class NoteStats implements InfoContributor {
  private SongRepository songRepository;
  private InstrumentRepository instrumentRepository;
  private PlaylistRepository playlistRepository;

  NoteStats(SongRepository songRepository, InstrumentRepository instrumentRepository, PlaylistRepository playlistRepository) {
    this.songRepository = songRepository;
    this.instrumentRepository = instrumentRepository;
    this.playlistRepository = playlistRepository;
  }

  @Override
  public void contribute(Info.Builder builder) {
    Map<String, Object> noteMap = new HashMap<>();
    noteMap.put("songs", songRepository.count());
    noteMap.put("instruments", instrumentRepository.count());
    noteMap.put("playlists", playlistRepository.count());
    builder.withDetail("notedb-stats", noteMap);
  }
}
