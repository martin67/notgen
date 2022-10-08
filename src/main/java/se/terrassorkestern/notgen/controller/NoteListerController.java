package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.service.NoteListerService;

@Slf4j
@Controller
@RequestMapping("/noteLister")
public class NoteListerController {

    private final NoteListerService noteListerService;


    public NoteListerController(NoteListerService noteListerService) {
        this.noteListerService = noteListerService;
    }

    @GetMapping(value = {"", "/"})
    public String noteLister() {
        log.info("Nu är vi i noteLister(Model model)");
        return "noteLister";
    }

    @GetMapping("/generate")
    public String noteListerGenerate() {
        log.info("Nu är vi i noteListerGenerate");
        noteListerService.createList();
        return "noteLister";
    }
}
