package se.terrassorkestern.notgen2.noteconverter;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.playlist.PlaylistRepository;
import se.terrassorkestern.notgen2.score.ScoreRepository;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class NoteStats implements InfoContributor {

    private final @NonNull ScoreRepository scoreRepository;
    private final @NonNull InstrumentRepository instrumentRepository;
    private final @NonNull PlaylistRepository playlistRepository;


    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("songs", scoreRepository.count());
        noteMap.put("instruments", instrumentRepository.count());
        noteMap.put("playlists", playlistRepository.count());
        builder.withDetail("notedb-stats", noteMap);
    }
}
