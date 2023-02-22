package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.util.List;


@Slf4j
@Controller
@RequestMapping("/search")
public class SearchController {
    private final ScoreRepository scoreRepository;

    public SearchController(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @PostMapping("/")
    public String find(@RequestParam("search") String searchTerm, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("q", searchTerm);
        return "redirect:";
    }

    @GetMapping("/")
    public String list(@RequestParam(name = "q", defaultValue = "") String searchTerm, Model model) {
        List<Score> scores = scoreRepository.searchBy(searchTerm, 1000, "title", "subTitle", "genre",
                "composer", "author", "arranger", "publisher", "comment", "text");
        model.addAttribute("scores", scores);
        return "search/list";
    }

}
