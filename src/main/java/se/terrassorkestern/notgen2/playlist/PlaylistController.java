package se.terrassorkestern.notgen2.playlist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/playlist")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;


    @GetMapping("/list")
    public String playlistList(Model model) {
        model.addAttribute("playlists", playlistRepository.findAll());
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
        playlist.getPlaylistEntries().add(new PlaylistEntry());
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
}
