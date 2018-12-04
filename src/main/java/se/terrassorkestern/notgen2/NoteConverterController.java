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
    private NoteConverter noteConverter;
    @Autowired
    private NoteConverterRunner noteConverterRunner;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private NoteConverterForm noteConverterForm;


    public NoteConverterController() {
        log.info("Constructor!");
    }


    @GetMapping("/noteConverter")
    public String noteLister(Model model) {
        log.info("Nu är vi i noteConverter");

        List<Song> songs = songRepository.findByOrderByTitle();
        model.addAttribute("songs", songs);
        model.addAttribute("noteConverterForm", noteConverterForm);

        return "noteConverter";
    }

    @PostMapping("/noteConverter")
    public String handlePost(@ModelAttribute("noteConverterForm") NoteConverterForm noteConverterForm) {
        log.info("Nu är vi i noteConverter post");

        // Starta konvertering!
        if (noteConverterForm.getAllSongs()) {
            if (noteConverterForm.getAsync())
                noteConverterRunner.convert(songRepository.findByOrderByTitle(), noteConverterForm.getUpload());
            else
                noteConverter.convert(songRepository.findByOrderByTitle(), noteConverterForm.getUpload());
        } else {
            if (noteConverterForm.getAsync())
                noteConverterRunner.convert(songRepository.findByIdInOrderByTitle(noteConverterForm.getSelectedSongs()), noteConverterForm.getUpload());
            else
                noteConverter.convert(songRepository.findByIdInOrderByTitle(noteConverterForm.getSelectedSongs()), noteConverterForm.getUpload());
        }
        return "noteConverter";
    }


    @GetMapping("/noteConverterGenerate")
    public String noteListerGenerate(Model model) {
        log.info("Nu är vi i noteConverterGenerate");

        noteConverter.convert(songRepository.findByOrderByTitle(), false);

        //model.addAttribute("instruments", instruments);

        return "noteConverter";
    }
}
