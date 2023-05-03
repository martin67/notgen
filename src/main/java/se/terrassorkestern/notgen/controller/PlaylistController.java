package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Playlist;
import se.terrassorkestern.notgen.model.PlaylistEntry;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.PlaylistPdfService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/playlist")
@SessionAttributes("playlist")
public class PlaylistController extends CommonController {

    public static final String ATTRIBUTE_ONE_PLAYLIST = "playlist";
    public static final String ATTRIBUTE_SETTINGS = "settings";
    public static final String ATTRIBUTE_INSTRUMENTS = "instruments";
    public static final String ATTRIBUTE_ALL_PLAYLISTS = "playlists";
    public static final String VIEW_PLAYLIST_LIST = "playlist/list";
    public static final String VIEW_PLAYLIST_VIEW = "playlist/view";
    public static final String VIEW_PLAYLIST_EDIT = "playlist/edit";
    public static final String REDIRECT_PLAYLIST_LIST = "redirect:/playlist/list";
    private final ActiveBand activeBand;
    private final PlaylistRepository playlistRepository;
    private final SettingRepository settingRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistPdfService playlistPdfService;
    private final ConverterService converterService;

    public PlaylistController(ActiveBand activeBand, PlaylistRepository playlistRepository, SettingRepository settingRepository,
                              InstrumentRepository instrumentRepository, PlaylistPdfService playlistPdfService,
                              ConverterService converterService) {
        this.activeBand = activeBand;
        this.playlistRepository = playlistRepository;
        this.settingRepository = settingRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistPdfService = playlistPdfService;
        this.converterService = converterService;
    }

    @GetMapping("/list")
    public String playlistList(Model model) {
        model.addAttribute(ATTRIBUTE_ALL_PLAYLISTS, getPlaylists());
        return VIEW_PLAYLIST_LIST;
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") UUID id, Model model) {
        Playlist playlist = getPlaylist(id);
        model.addAttribute(ATTRIBUTE_ONE_PLAYLIST, playlist);
        List<Instrument> sortedInstruments = playlist.getSetting().getInstruments().stream().sorted(Comparator.comparing(Instrument::getSortOrder)).toList();
        model.addAttribute(ATTRIBUTE_INSTRUMENTS, sortedInstruments);
        return VIEW_PLAYLIST_VIEW;
    }

    @GetMapping("/edit")
    public String playlistEdit(@RequestParam("id") UUID id, Model model) {
        model.addAttribute(ATTRIBUTE_ONE_PLAYLIST, getPlaylist(id));
        model.addAttribute(ATTRIBUTE_SETTINGS, settingRepository.findByBand(activeBand.getBand()));
        model.addAttribute(ATTRIBUTE_INSTRUMENTS, instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        int selectedInstrument = 0;
        model.addAttribute("selectedInstrument", selectedInstrument);
        return VIEW_PLAYLIST_EDIT;
    }

    @GetMapping("/create")
    public String playlistNew(Model model) {
        model.addAttribute(ATTRIBUTE_ONE_PLAYLIST, new Playlist());
        model.addAttribute(ATTRIBUTE_SETTINGS, settingRepository.findByBand(activeBand.getBand()));
        model.addAttribute(ATTRIBUTE_INSTRUMENTS, instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return VIEW_PLAYLIST_EDIT;
    }

    @GetMapping("/delete")
    public String playlistDelete(@RequestParam("id") UUID id, SessionStatus sessionStatus) {
        Playlist playlist = getPlaylist(id);
        log.info("Tar bort låtlista {} [{}]", playlist.getName(), playlist.getId());
        playlistRepository.delete(playlist);
        sessionStatus.setComplete();
        return REDIRECT_PLAYLIST_LIST;
    }

    @GetMapping("/copy")
    public String playlistCopy(@RequestParam("id") UUID id, SessionStatus sessionStatus) {
        Playlist playlist = getPlaylist(id);
        log.info("Kopierar låtlista {} [{}]", playlist.getName(), playlist.getId());
        Playlist newPlaylist = playlist.copy();
        playlistRepository.save(newPlaylist);
        sessionStatus.setComplete();
        return REDIRECT_PLAYLIST_LIST;
    }

    @PostMapping("/save")
    public String playlistSave(@Valid @ModelAttribute Playlist playlist, Errors errors, SessionStatus sessionStatus) {
        if (errors.hasErrors()) {
            return VIEW_PLAYLIST_EDIT;
        }
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_PLAYLIST"))) {
            log.info("Sparar låtlista {} [{}]", playlist.getName(), playlist.getId());
            playlist.setBand(activeBand.getBand());
            playlistRepository.save(playlist);
        }
        sessionStatus.setComplete();
        return REDIRECT_PLAYLIST_LIST;
    }

    @PostMapping(value = "/save", params = {"addRow"})
    public String addRow(@ModelAttribute("playlist") Playlist playlist) {
        PlaylistEntry playlistEntry = new PlaylistEntry();
        playlistEntry.setSortOrder(playlist.getPlaylistEntries().size() + 1);
        playlist.getPlaylistEntries().add(playlistEntry);
        return VIEW_PLAYLIST_EDIT;
    }

    @PostMapping(value = "/save", params = {"deleteRow"})
    public String deleteRow(@ModelAttribute("playlist") Playlist playlist,
                            @RequestParam("deleteRow") int rowIndex) {
        playlist.getPlaylistEntries().remove(rowIndex);
        return VIEW_PLAYLIST_EDIT;
    }

    @PostMapping(value = "/save", params = {"createPack"})
    public ResponseEntity<InputStreamResource> createPack(@ModelAttribute("playlist") Playlist playlist,
                                                          @RequestParam String instrumentId) {
        UUID id = UUID.fromString(instrumentId);
        log.debug("Startar createPack för instrument id {} ", id);

        Instrument instrument = instrumentRepository.findByBandAndId(activeBand.getBand(), id).orElseThrow();
        try (InputStream is = converterService.assemble(playlist, instrument)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=playlist.pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/createPdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> playlistCreatePdf(@RequestParam("id") UUID id) {
        Playlist playlist = getPlaylist(id);
        ByteArrayInputStream bis;
        try {
            bis = playlistPdfService.create(playlist);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=playlist.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

}
