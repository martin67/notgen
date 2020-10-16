package se.terrassorkestern.notgen2.noteconverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen2.score.Score;
import se.terrassorkestern.notgen2.score.ScoreRepository;

import java.util.List;

@Controller
@RequestMapping("/noteConverter")
public class NoteConverterController {
    static final Logger log = LoggerFactory.getLogger(NoteConverterController.class);

    private final ScoreRepository scoreRepository;
    private final NoteConverterService noteConverterService;


    public NoteConverterController(ScoreRepository scoreRepository, NoteConverterService noteConverterService) {
        this.scoreRepository = scoreRepository;
        this.noteConverterService = noteConverterService;
    }

    @GetMapping(value = {"", "/"})
    public String noteConverter(Model model) {
        log.info("Nu är vi i noteConverter");

        List<Score> scores = scoreRepository.findByOrderByTitle();
        model.addAttribute("scores", scores);
        model.addAttribute("noteConverterDto", new NoteConverterDto());

        return "noteConverter";
    }

    @PostMapping(value = "/convert", params = {"convertNotes"})
    public String handlePost(@ModelAttribute("noteConverterDto") NoteConverterDto noteConverterDto) {

        log.info("Nu är vi i noteConverter post");

        // Starta konvertering!
        if (noteConverterDto.isAllScores()) {
            noteConverterService.convert(scoreRepository.findByOrderByTitle(), noteConverterDto.isUpload());
        } else {
            noteConverterService.convert(scoreRepository.findByIdInOrderByTitle(noteConverterDto.getSelectedScores()),
                    noteConverterDto.isUpload());
        }

        return "redirect:/noteConverter";
    }

    @PostMapping(value = "/convert", params = {"createPacks"})
    public String createPacks(@ModelAttribute("noteConverterDto") NoteConverterDto noteConverterDto) {

        log.info("Skapar instrumentpackar");

        noteConverterService.createInstrumentPacks(noteConverterDto.isUpload());

        return "redirect:/noteConverter";
    }

}
