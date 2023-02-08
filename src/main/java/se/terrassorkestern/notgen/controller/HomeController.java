package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {

    @Value("${git.commit.id.abbrev}")
    private String GitCommit;
    @Value("${git.commit.time}")
    private String GitTime;

    @GetMapping("/")
    public String home() {
        log.info("git.commit.id.abbrev: {}", GitCommit);
        log.info("git.commit.time: {}", GitTime);
        return "home";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}