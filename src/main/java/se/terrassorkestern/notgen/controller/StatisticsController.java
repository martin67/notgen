package se.terrassorkestern.notgen.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.service.StatisticsService;

import java.io.IOException;

@Slf4j
@Controller
@RequestMapping("/statistics")
public class StatisticsController {

    private static final String TEXT_CSV = "text/csv";
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

    @GetMapping(value = {"/scorelist"})
    public void list(HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType(TEXT_CSV);
        statisticsService.writeScoresToCsv(servletResponse.getWriter());
    }

    @GetMapping(value = {"/fullscorelist"})
    public void fullList(HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType(TEXT_CSV);
        statisticsService.writeFullScoresToCsv(servletResponse.getWriter());
    }

    @GetMapping(value = {"/unscanned"})
    public void unscanned(HttpServletResponse servletResponse) throws IOException {
        statisticsService.writeUnscannedToCsv(servletResponse.getWriter());
    }
}