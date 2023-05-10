package se.terrassorkestern.notgen.model;

import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class NoteStats implements InfoContributor {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistRepository playlistRepository;

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> noteMap = new HashMap<>();
        noteMap.put("songs", scoreRepository.count());
        noteMap.put("instruments", instrumentRepository.count());
        noteMap.put("playlists", playlistRepository.count());
        builder.withDetail("notedb-stats", noteMap);
    }
}
