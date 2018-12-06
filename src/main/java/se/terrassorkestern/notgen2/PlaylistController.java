package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
        Playlist playlist = playlistRepository.findById(id).get();
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
    public String playlistSave(@ModelAttribute Playlist playlist, BindingResult bindingResult, Model model) {
        log.info("Sparar låtlista " + playlist.getName() + " [" + playlist.getId() + "]");
        playlistRepository.save(playlist);
        return "redirect:/playlist/list";
    }

}
