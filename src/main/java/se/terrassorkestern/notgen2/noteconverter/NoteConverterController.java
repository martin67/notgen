package se.terrassorkestern.notgen2.noteconverter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/noteConverter")
public class NoteConverterController {

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private NoteConverterService noteConverterService;


    @GetMapping(value = {"", "/"})
    public String noteConverter(Model model) {
        log.info("Nu är vi i noteConverter");

        List<Song> songs = songRepository.findByOrderByTitle();
        model.addAttribute("songs", songs);
        model.addAttribute("noteConverterDto", new NoteConverterDto());

        return "noteConverter";
    }

    @PostMapping("/convert")
    public String handlePost(@ModelAttribute("noteConverterDto") NoteConverterDto noteConverterDto) {

        log.info("Nu är vi i noteConverter post");

        // Starta konvertering!
        if (noteConverterDto.getAllSongs()) {
            noteConverterService.convert(songRepository.findByOrderByTitle(), noteConverterDto.getUpload());
        } else {
            noteConverterService.convert(songRepository.findByIdInOrderByTitle(noteConverterDto.getSelectedSongs()),
                    noteConverterDto.getUpload());
        }

        return "redirect:/noteConverter";
    }

}
