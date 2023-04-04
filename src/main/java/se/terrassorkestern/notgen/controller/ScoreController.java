package se.terrassorkestern.notgen.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/score")
@SessionAttributes("score")
public class ScoreController extends CommonController {
    @Value("${notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${notgen.ocr.songids:0}")
    private String ocrSongIds;

    private final ActiveBand activeBand;
    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final ConverterService converterService;
    private final SongOcrService songOcrService;

    public ScoreController(ActiveBand activeBand, ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                           ConverterService converterService,
                           SongOcrService songOcrService) {
        this.activeBand = activeBand;
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.converterService = converterService;
        this.songOcrService = songOcrService;
    }

    @GetMapping("/list")
    public String songList(Model model) {
        model.addAttribute("scores", getScores());
        return "score/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Integer id) {
        Score score = getScore(id);
        log.info("Tar bort låt {} [{}]", score.getTitle(), score.getId());
        scoreRepository.delete(score);
        return "redirect:/score/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("score", getScore(id));
        model.addAttribute("settings", getSettings());
        return "score/view";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") Integer id, Model model) {
        Score score = getScore(id);
        model.addAttribute("score", score);
        // Check if the score has a song instrument. Only one for now
        if (enableOcr) {
            int songId = Integer.parseInt(ocrSongIds);
            if (score.getInstruments().stream().anyMatch(instrument -> instrument.getId() == songId)) {
                model.addAttribute("doSongOcr", "true");
            } else {
                model.addAttribute("doSongOcr", "false");
            }
        }
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Score score = new Score();
        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        for (Instrument instrument : getInstruments()) {
            score.getScoreParts().add(new ScorePart(score, instrument));
        }
        model.addAttribute("score", score);
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute Score score, @RequestPart("file") MultipartFile file, @RequestPart("arr_id") Integer id) {
        log.info("upload: {}, score id: {}", file.getOriginalFilename(), score.getId());

        //storageService.uploadArrangement();
        return "score/edit";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Score score, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("allInstruments", getInstruments());
            return "score/edit";
        }
        log.info("Sparar låt {} [{}]", score.getTitle(), score.getId());
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
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
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
            model.addAttribute("allInstruments", getInstruments());
        } catch (NumberFormatException ignore) {
        }
        return "score/edit";
    }

    @GetMapping("/convert")
    public String convert(@RequestParam("id") int id) throws IOException, InterruptedException {
        converterService.convert(List.of(getScore(id)));
        return "redirect:/score/list";
    }

    @GetMapping("/edit/ocr")
    public @ResponseBody
    String ocr(@RequestParam("id") int id) throws Exception {
        return songOcrService.process(getScore(id));
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
        if (isSuperAdmin()) {
            return scoreRepository.getAllPublishers();
        } else {
            return scoreRepository.getAllPublishersByBand(activeBand.getBand());
        }
    }

}