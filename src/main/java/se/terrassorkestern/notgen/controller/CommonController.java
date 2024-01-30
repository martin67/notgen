package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
public class CommonController {

    @Autowired
    private ActiveBand activeBand;
    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private SettingRepository settingRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private ScoreRepository scoreRepository;


    boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    boolean isSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    Instrument getInstrument(UUID id) {
        Instrument instrument;
        if (isSuperAdmin()) {
            instrument = instrumentRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Instrument %s not found", id)));
        } else {
            instrument = instrumentRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Instrument %s not found", id)));
        }
        return instrument;
    }

    List<Instrument> getInstruments() {
        List<Instrument> instruments;
        if (isSuperAdmin()) {
            instruments = instrumentRepository.findByOrderBySortOrder();
        } else {
            instruments = instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand());
        }
        return instruments;
    }

    Setting getSetting(UUID id) {
        Setting setting;
        if (isSuperAdmin()) {
            setting = settingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %s not found", id)));
        } else {
            setting = settingRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %s not found", id)));
        }
        return setting;
    }

    List<Setting> getSettings() {
        List<Setting> settings;
        if (isSuperAdmin()) {
            settings = settingRepository.findAll();
        } else {
            settings = settingRepository.findByBand(activeBand.getBand());
        }
        return settings;
    }

    Playlist getPlaylist(UUID id) {
        Playlist playlist;
        if (isSuperAdmin()) {
            playlist = playlistRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Playlist %s not found", id)));
        } else {
            playlist = playlistRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Playlist %s not found", id)));
        }
        return playlist;
    }

    List<Playlist> getPlaylists() {
        List<Playlist> playlists;
        if (isSuperAdmin()) {
            playlists = playlistRepository.findAllByOrderByDateDesc();
        } else {
            playlists = playlistRepository.findByBandOrderByDateDesc(activeBand.getBand());
        }
        return playlists;
    }

    Score getScore(UUID id) {
        Score score;
        if (isSuperAdmin()) {
            score = scoreRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Score %s not found", id)));
        } else {
            score = scoreRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Score %s not found", id)));
        }
        return score;
    }

    List<Score> getScores() {
        List<Score> scores;
        if (isSuperAdmin()) {
            scores = scoreRepository.findByOrderByTitle();
        } else {
            scores = scoreRepository.findByBandOrderByTitleAsc(activeBand.getBand());
        }
        return scores;
    }

}
