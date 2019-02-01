package se.terrassorkestern.notgen2.playlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;

@Slf4j
@Controller
@RequestMapping("/playlist")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private PlaylistPdfService playlistPdfService;

    @Autowired
    private PlaylistPackService playlistPackService;


    @GetMapping("/list")
    public String playlistList(Model model) {
        model.addAttribute("playlists", playlistRepository.findAllByOrderByDateDesc());
        return "playlistList";
    }

    @GetMapping("/edit")
    public String playlistEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("playlist", playlistRepository.findById(id).get());
        model.addAttribute("instruments", instrumentRepository.findByStandardIsTrueOrderBySortOrder());
        Integer selectedInstrument = 0;
        model.addAttribute("selectedInstrument", selectedInstrument);
        return "playlistEdit";
    }

    @GetMapping("/new")
    public String playlistNew(Model model) {
        model.addAttribute("playlist", new Playlist());
        model.addAttribute("instruments", instrumentRepository.findByStandardIsTrueOrderBySortOrder());
        return "playlistEdit";
    }

    @GetMapping("/delete")
    public String playlistDelete(@RequestParam("id") Integer id, Model model,
                                 @AuthenticationPrincipal User user) {
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_PLAYLIST"))) {
            Playlist playlist = playlistRepository.findById(id).get();
            log.info("Tar bort låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
            playlistRepository.delete(playlist);
        }
        return "redirect:/playlist/list";
    }

    @GetMapping("/copy")
    public String playlistCopy(@RequestParam("id") Integer id, Model model,
                               @AuthenticationPrincipal User user) {
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_PLAYLIST"))) {
            Playlist playlist = playlistRepository.findById(id).get();
            log.info("Kopierar låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
            Playlist newPlaylist = playlist.copy();
            playlistRepository.save(newPlaylist);
        }
        return "redirect:/playlist/list";
    }

    @PostMapping("/save")
    public String playlistSave(@Valid @ModelAttribute Playlist playlist, Errors errors,
                               @AuthenticationPrincipal User user) {
        if (errors.hasErrors()) {
            return "playlistEdit";
        }
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_PLAYLIST"))) {
            log.info("Sparar låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
            playlistRepository.save(playlist);
        }
        return "redirect:/playlist/list";
    }

    @PostMapping(value = "/save", params = {"addRow"})
    public String addRow(final Playlist playlist, final BindingResult bindingResult, Model model) {
        PlaylistEntry playlistEntry = new PlaylistEntry();
        playlistEntry.setSortOrder(playlist.getPlaylistEntries().size() + 1);
        playlist.getPlaylistEntries().add(playlistEntry);
        model.addAttribute("playlist", playlist);
        return "playlistEdit";
    }

    @PostMapping(value = "/save", params = {"deleteRow"})
    public String deleteRow(final Playlist playlist, final BindingResult bindingResult, Model model, final HttpServletRequest req) {
        final Integer playlistPartId = Integer.valueOf(req.getParameter("deleteRow"));
        playlist.getPlaylistEntries().remove(playlistPartId.intValue());
        model.addAttribute("playlist", playlist);
        return "playlistEdit";
    }

    @PostMapping(value = "/save", params = {"createPack"})
    public ResponseEntity<InputStreamResource> createPack(final Playlist playlist,
                                                          final BindingResult bindingResult,
                                                          Model model,
                                                          final HttpServletRequest req) {

        final Integer selectedInstrumentId = Integer.valueOf(req.getParameter("selectedInstrument"));

        log.debug("Startar createPack för instrument id " + selectedInstrumentId);
        String fileName;
        fileName = playlistPackService.createPack(playlist, instrumentRepository.getOne(selectedInstrumentId));

        File file = new File(fileName);


        InputStreamResource isr = null;
        try {
            isr = new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=playlistpack.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(isr);
    }


    @GetMapping(value = "/createPdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> playlistCreatePdf(@RequestParam("id") Integer id, Model model) {
        Playlist playlist = playlistRepository.findById(id).get();

        //ByteArrayInputStream bis = GeneratePdfReport.citiesReport(cities);
        ByteArrayInputStream bis = null;
        try {
            bis = playlistPdfService.create(playlist);
        } catch (IOException e) {
            e.printStackTrace();
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
