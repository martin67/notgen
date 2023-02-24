package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {

    @Value("${git.commit.id.abbrev}")
    private String gitCommit;
    @Value("${git.commit.time}")
    private String gitTime;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        log.info("git.commit.id.abbrev: {}", gitCommit);
        log.info("git.commit.time: {}", gitTime);
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}