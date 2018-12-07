package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Slf4j
@Controller
public class NoteConverterController {

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private NoteConverterService noteConverterService;


    public NoteConverterController() {
        log.info("Constructor!");
    }


    @GetMapping("/noteConverter")
    public String noteLister(Model model) {
        log.info("Nu är vi i noteConverter");

        List<Song> songs = songRepository.findByOrderByTitle();
        model.addAttribute("songs", songs);
        model.addAttribute("noteConverterForm", new NoteConverterForm());

        return "noteConverter";
    }

    @PostMapping("/noteConverter")
    public String handlePost(@ModelAttribute("noteConverterForm") NoteConverterForm noteConverterForm) {
        log.info("Nu är vi i noteConverter post");


        noteConverterService.convert(noteConverterForm.getSelectedSongs(),
                noteConverterForm.getAllSongs(), noteConverterForm.getUpload());

        return "redirect:/noteConverter";
    }

}
