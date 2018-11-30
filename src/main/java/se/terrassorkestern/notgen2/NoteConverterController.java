package se.terrassorkestern.notgen2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
public class NoteConverterController {

    private final Logger log = LoggerFactory.getLogger(NoteConverterController.class);

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

    @RequestMapping("/noteConverter")
    public String noteLister(Model model) {
        log.info("Nu är vi i noteConverter");

        List<Song> songs = songRepository.findByOrderByTitle();
        model.addAttribute("songs", songs);
        model.addAttribute("noteConverterForm", noteConverterForm);

        return "noteConverter";
    }

    @RequestMapping(value="/noteConverter", method=RequestMethod.POST)
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


    @RequestMapping("/noteConverterGenerate")
    public String noteListerGenerate(Model model) {
        log.info("Nu är vi i noteConverterGenerate");

        noteConverter.convert(songRepository.findByOrderByTitle(), false);

        //model.addAttribute("instruments", instruments);

        return "noteConverter";
    }
}
