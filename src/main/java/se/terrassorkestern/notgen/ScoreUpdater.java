package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;

import java.util.UUID;

@Slf4j
@Component
@Transactional
public class ScoreUpdater {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final BandRepository bandRepository;
    private final NgFileRepository ngFileRepository;
    private final PlaylistRepository playlistRepository;
    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;

    public ScoreUpdater(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                        BandRepository bandRepository, NgFileRepository ngFileRepository,
                        PlaylistRepository playlistRepository, PrivilegeRepository privilegeRepository,
                        RoleRepository roleRepository, SettingRepository settingRepository,
                        UserRepository userRepository) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.bandRepository = bandRepository;
        this.ngFileRepository = ngFileRepository;
        this.playlistRepository = playlistRepository;
        this.privilegeRepository = privilegeRepository;
        this.roleRepository = roleRepository;
        this.settingRepository = settingRepository;
        this.userRepository = userRepository;
    }


    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("********* Score updater");
        for (Score score : scoreRepository.findAll()) {
            for (Arrangement arrangement : score.getArrangements()) {
                if (score.getFiles().isEmpty()) {
                    score.getFiles().add(arrangement.getFile());
                    log.info("Adding file to score {} ({})", score.getTitle(), score.getId());
                }
            }
            scoreRepository.save(score);
        }


        for (Band band : bandRepository.findAll()) {
            if (band.getUuid() == null) {
                band.setUuid(UUID.randomUUID());
                bandRepository.save(band);
            }
        }

        for (Instrument instrument : instrumentRepository.findAll()) {
            if (instrument.getUuid() == null) {
                instrument.setUuid(UUID.randomUUID());
            }
            instrument.setBand_uuid(instrument.getBand().getUuid());
            instrumentRepository.save(instrument);
        }

        for (Setting setting : settingRepository.findAll()) {
            if (setting.getUuid() == null) {
                setting.setUuid(UUID.randomUUID());
            }
            setting.setBand_uuid(setting.getBand().getUuid());
            settingRepository.save(setting);
        }

        for (NgFile ngFile : ngFileRepository.findAll()) {
            if (ngFile.getUuid() == null) {
                ngFile.setUuid(UUID.randomUUID());
                ngFileRepository.save(ngFile);
            }
        }

        for (Score score : scoreRepository.findAll()) {
            if (score.getUuid() == null) {
                score.setUuid(UUID.randomUUID());
            }
            score.setBand_uuid(score.getBand().getUuid());
            for (Arrangement arrangement : score.getArrangements()) {
                if (arrangement.getUuid() == null) {
                    arrangement.setUuid(UUID.randomUUID());
                }
                arrangement.setScore_uuid(score.getUuid());
                if (arrangement.getFile() != null) {
                    arrangement.setFile_uuid(arrangement.getFile().getUuid());
                }
                for (ArrangementPart arrangementPart : arrangement.getArrangementParts()) {
                    arrangementPart.setArrangement_uuid(arrangement.getUuid());
                    arrangementPart.setInstrument_uuid(arrangementPart.getInstrument().getUuid());
                }
            }
            if (!score.getArrangements().isEmpty()) {
                score.setDefaultArrangement_uuid(score.getDefaultArrangement().getUuid());
            }
            scoreRepository.save(score);
        }

        for (Playlist playlist : playlistRepository.findAll()) {
            if (playlist.getUuid() == null) {
                playlist.setUuid(UUID.randomUUID());
            }
            playlist.setBand_uuid(playlist.getBand().getUuid());
            playlist.setSetting_uuid(playlist.getSetting().getUuid());
            for (PlaylistEntry playlistEntry : playlist.getPlaylistEntries()) {
                if (playlistEntry.getUuid() == null) {
                    playlistEntry.setUuid(UUID.randomUUID());
                }
            }
            playlistRepository.save(playlist);
        }

        for (Privilege privilege : privilegeRepository.findAll()) {
            if (privilege.getUuid() == null) {
                privilege.setUuid(UUID.randomUUID());
                privilegeRepository.save(privilege);
            }
        }

        for (Role role : roleRepository.findAll()) {
            if (role.getUuid() == null) {
                role.setUuid(UUID.randomUUID());
                roleRepository.save(role);
            }
        }


        for (User user : userRepository.findAll()) {
            if (user.getUuid() == null) {
                user.setUuid(UUID.randomUUID());
            }
            user.setRole_uuid(user.getRole().getUuid());
            userRepository.save(user);
        }
    }

}
