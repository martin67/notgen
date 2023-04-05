package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;

import java.util.List;

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    Instrument getInstrument(int id) {
        Instrument instrument;
        if (isSuperAdmin()) {
            instrument = instrumentRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Instrument %d not found", id)));
        } else {
            instrument = instrumentRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Instrument %d not found", id)));
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

    Setting getSetting(int id) {
        Setting setting;
        if (isSuperAdmin()) {
            setting = settingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %d not found", id)));
        } else {
            setting = settingRepository.findByIdAndBand(id, activeBand.getBand())
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %d not found", id)));
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

    Playlist getPlaylist(int id) {
        Playlist playlist;
        if (isSuperAdmin()) {
            playlist = playlistRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Playlist %d not found", id)));
        } else {
            playlist = playlistRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Playlist %d not found", id)));
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

    Score getScore(int id) {
        Score score;
        if (isSuperAdmin()) {
            score = scoreRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Score %d not found", id)));
        } else {
            score = scoreRepository.findByBandAndId(activeBand.getBand(), id)
                    .orElseThrow(() -> new NotFoundException(String.format("Score %d not found", id)));
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
