package se.terrassorkestern.notgen2.playlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;

@Slf4j
@Controller
@RequestMapping("/playlist")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;


    @GetMapping("/list")
    public String playlistList(Model model) {
        model.addAttribute("playlists", playlistRepository.findAllByOrderByDateDesc());
        return "playlistList";
    }

    @GetMapping("/edit")
    public String playlistEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("playlist", playlistRepository.findById(id).get());
        return "playlistEdit";
    }

    @GetMapping("/new")
    public String playlistNew(Model model) {
        model.addAttribute("playlist", new Playlist());
        return "playlistEdit";
    }

    @GetMapping("/delete")
    public String playlistDelete(@RequestParam("id") Integer id, Model model) {
        Playlist playlist = playlistRepository.findById(id).get();
        log.info("Tar bort låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
        playlistRepository.delete(playlist);
        return "redirect:/playlist/list";
    }

    @GetMapping("/copy")
    public String playlistCopy(@RequestParam("id") Integer id, Model model) {
        Playlist playlist = playlistRepository.findById(id).get();
        log.info("Kopierar låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
        Playlist newPlaylist = playlist.copy();
        playlistRepository.save(newPlaylist);
        return "redirect:/playlist/list";
    }

    @PostMapping("/save")
    public String playlistSave(@Valid @ModelAttribute Playlist playlist, Errors errors) {
        if (errors.hasErrors()) {
            return "playlistEdit";
        }
        log.info("Sparar låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
        playlistRepository.save(playlist);
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

    @GetMapping(value = "/createPdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> playlistCreatePdf(@RequestParam("id") Integer id, Model model) {
        Playlist playlist = playlistRepository.findById(id).get();

        //ByteArrayInputStream bis = GeneratePdfReport.citiesReport(cities);
        ByteArrayInputStream bis = new PlaylistPdfCreator().create(playlist);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=playlist.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

}
