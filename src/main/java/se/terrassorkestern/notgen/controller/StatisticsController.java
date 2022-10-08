package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.service.StatisticsService;

@Slf4j
@Controller
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;


    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping(value = {"", "/"})
    public String statistics(Model model) {

        Statistics statistics = statisticsService.getStatistics();
        model.addAttribute("statistics", statistics);
        model.addAttribute("numberOfSongs", statistics.getNumberOfSongs());
        return "statistics";
    }
}