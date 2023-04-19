package se.terrassorkestern.notgen.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.PlaylistPdfService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/playlist")
public class PlaylistController extends CommonController {

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
        model.addAttribute("playlists", getPlaylists());
        return "playlist/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") UUID id, Model model) {
        model.addAttribute("playlist", getPlaylist(id));
        model.addAttribute("instruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "playlist/view";
    }

    @GetMapping("/edit")
    public String playlistEdit(@RequestParam("id") UUID id, Model model) {
        model.addAttribute("playlist", getPlaylist(id));
        model.addAttribute("settings", settingRepository.findByBand(activeBand.getBand()));
        model.addAttribute("instruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        int selectedInstrument = 0;
        model.addAttribute("selectedInstrument", selectedInstrument);
        return "playlist/edit";
    }

    @GetMapping("/create")
    public String playlistNew(Model model) {
        model.addAttribute("playlist", new Playlist());
        model.addAttribute("settings", settingRepository.findByBand(activeBand.getBand()));
        model.addAttribute("instruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "playlist/edit";
    }

    @GetMapping("/delete")
    public String playlistDelete(@RequestParam("id") UUID id) {
        Playlist playlist = getPlaylist(id);
        log.info("Tar bort låtlista {} [{}]", playlist.getName(), playlist.getId());
        playlistRepository.delete(playlist);
        return "redirect:/playlist/list";
    }

    @GetMapping("/copy")
    public String playlistCopy(@RequestParam("id") UUID id) {
        Playlist playlist = getPlaylist(id);
        log.info("Kopierar låtlista {} [{}]", playlist.getName(), playlist.getId());
        Playlist newPlaylist = playlist.copy();
        playlistRepository.save(newPlaylist);
        return "redirect:/playlist/list";
    }

    @PostMapping("/save")
    public String playlistSave(@Valid @ModelAttribute Playlist playlist, Errors errors) {
        if (errors.hasErrors()) {
            return "playlist/edit";
        }
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_PLAYLIST"))) {
            log.info("Sparar låtlista {} [{}]", playlist.getName(), playlist.getId());
            playlist.setBand(activeBand.getBand());
            playlistRepository.save(playlist);
        }
        return "redirect:/playlist/list";
    }

    @PostMapping(value = "/save", params = {"addRow"})
    public String addRow(final Playlist playlist, Model model) {
        PlaylistEntry playlistEntry = new PlaylistEntry();
        playlistEntry.setSortOrder(playlist.getPlaylistEntries().size() + 1);
        playlist.getPlaylistEntries().add(playlistEntry);
        model.addAttribute("playlist", playlist);
        return "playlist/edit";
    }

    @PostMapping(value = "/save", params = {"deleteRow"})
    public String deleteRow(final Playlist playlist, Model model, final HttpServletRequest req) {
        try {
            int playlistPartId = Integer.parseInt(req.getParameter("deleteRow"));
            playlist.getPlaylistEntries().remove(playlistPartId);
            model.addAttribute("playlist", playlist);
        } catch (NumberFormatException ignored) {
        }
        return "playlist/edit";
    }

    @PostMapping(value = "/save", params = {"createPack"})
    public ResponseEntity<InputStreamResource> createPack(final Playlist playlist,
                                                          final HttpServletRequest req) {
        UUID id;
        try {
            id = UUID.fromString(req.getParameter("selectedInstrument"));
        } catch (NumberFormatException e) {
            throw new NotFoundException("Instrument not found");
        }

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
