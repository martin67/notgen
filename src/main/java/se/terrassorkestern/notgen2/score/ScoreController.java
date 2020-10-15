package se.terrassorkestern.notgen2.score;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen2.exceptions.NotFoundException;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/score")
@SessionAttributes("score")
public class ScoreController {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;


    public ScoreController(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/list")
    public String songList(Model model) {
        model.addAttribute("scores", scoreRepository.findByOrderByTitle());
        return "scoreList";
    }

    @GetMapping("/delete")
    public String songDelete(@RequestParam("id") Integer id) {
        Score score = scoreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Score %d not found", id)));
        log.info("Tar bort låt " + score.getTitle() + " [" + score.getId() + "]");
        scoreRepository.delete(score);
        return "redirect:/score/list";
    }

    @GetMapping("/edit")
    public String songEdit(@RequestParam("id") Integer id, Model model) {
        Score score = scoreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Score %d not found", id)));
        model.addAttribute("score", score);
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        return "scoreEdit";
    }

    @GetMapping("/new")
    public String songNew(Model model) {
        Score score = new Score();
        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        for (Instrument instrument : instrumentRepository.findAll()) {
            score.getScoreParts().add(new ScorePart(score, instrument));
        }
        model.addAttribute("score", score);
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        return "scoreEdit";
    }

    @PostMapping("/save")
    public String songSave(@Valid @ModelAttribute Score score, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("allInstruments", instrumentRepository.findAll());
            return "scoreEdit";
        }
        log.info("Sparar låt " + score.getTitle() + " [" + score.getId() + "]");
        // scorePart måste fixas till efter formuläret
        for (ScorePart scorePart : score.getScoreParts()) {
            Instrument instrument = scorePart.getInstrument();
            scorePart.setId(new ScorePartId(score.getId(), instrument.getId()));
            scorePart.setScore(score);
            scorePart.setInstrument(instrumentRepository.findById(instrument.getId()).orElse(null));
        }
        scoreRepository.save(score);
        return "redirect:/score/list";
    }

    @PostMapping(value = "/save", params = {"addRow"})
    public String addRow(final Score score, Model model) {
        score.getScoreParts().add(new ScorePart());
        model.addAttribute("score", score);
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        return "scoreEdit";
    }

    @PostMapping(value = "/save", params = {"deleteRow"})
    public String deleteRow(final Score score, Model model, final HttpServletRequest req) {
        try {
            int scorePartId = Integer.parseInt(req.getParameter("deleteRow"));
            if (scorePartId < score.getScoreParts().size()) {
                score.getScoreParts().remove(scorePartId);
            } else {
                log.warn("Trying to remove non-existing score part {}", scorePartId);
            }
            model.addAttribute("score", score);
            model.addAttribute("allInstruments", instrumentRepository.findAll());
        } catch (NumberFormatException ignore) {
        }
        return "scoreEdit";
    }

    @GetMapping(value = "/scores.json")
    public @ResponseBody
    List<String> getTitleSuggestions() {
        return scoreRepository.getAllTitles();
    }

    @GetMapping(value = "/genres.json")
    public @ResponseBody
    List<String> getGenreSuggestions() {
        return scoreRepository.getAllGenres();
    }

    @GetMapping(value = "/composers.json")
    public @ResponseBody
    List<String> getComposerSuggestions() {
        return scoreRepository.getAllComposers();
    }

    @GetMapping(value = "/authors.json")
    public @ResponseBody
    List<String> getAuthorSuggestions() {
        return scoreRepository.getAllAuthors();
    }

    @GetMapping(value = "/arrangers.json")
    public @ResponseBody
    List<String> getArrangerSuggestions() {
        return scoreRepository.getAllArrangers();
    }

    @GetMapping(value = "/publishers.json")
    public @ResponseBody
    List<String> getPublisherSuggestions() {
        return scoreRepository.getAllPublishers();
    }

}