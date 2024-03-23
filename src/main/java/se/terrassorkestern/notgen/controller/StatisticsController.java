package se.terrassorkestern.notgen.controller;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import se.terrassorkestern.notgen.model.Playlist;
import se.terrassorkestern.notgen.repository.LinkRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.service.StatisticsService;

import java.io.IOException;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/statistics")
public class StatisticsController {

    public static final String TEXT_CSV = "text/csv";
    private final StatisticsService statisticsService;
    private final LinkRepository linkRepository;
    private final PlaylistRepository playlistRepository;
    private final EntityManager entityManager;

    public StatisticsController(StatisticsService statisticsService, LinkRepository linkRepository,
                                PlaylistRepository playlistRepository, EntityManager entityManager) {
        this.statisticsService = statisticsService;
        this.linkRepository = linkRepository;
        this.playlistRepository = playlistRepository;
        this.entityManager = entityManager;
    }

    @GetMapping(value = {"", "/"})
    public String statistics(Model model) {
        var statistics = statisticsService.getStatistics();
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

    @GetMapping(value = {"/fullplaylist"})
    public void fullPlayList(@RequestParam(name = "id") UUID id,  HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType(TEXT_CSV);
        Playlist playlist = playlistRepository.getReferenceById(id);
        statisticsService.writeFullScoresToCsv(servletResponse.getWriter(), playlist);
    }

    @GetMapping(value = {"/unscanned"})
    public void unscanned(HttpServletResponse servletResponse) throws IOException {
        statisticsService.writeUnscannedToCsv(servletResponse.getWriter());
    }

    @GetMapping("/listen")
    public String listAudio(Model model) {
        //List<Link> links = entityManager.createQuery("select * from link").getResultList();
        var links = linkRepository.findByOrderByName();
        Map<String, List<String>> linksAndSongs = new HashMap<>();
        for (var link : links) {
            linksAndSongs.putIfAbsent(link.getName(), new ArrayList<>());
            var ls = linksAndSongs.get(link.getName());
            ls.add(link.getScore().getTitle());
            log.info("Adding");
        }
        model.addAttribute("links", linksAndSongs);
        return "listen";
    }
}