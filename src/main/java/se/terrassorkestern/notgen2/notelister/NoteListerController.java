package se.terrassorkestern.notgen2.notelister;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/noteLister")
public class NoteListerController {

    @Autowired
    private NoteListerService noteListerService;


    @GetMapping(value = {"", "/"})
    public String noteLister(Model model) {
        log.info("Nu är vi i noteLister(Model model)");
        return "noteLister";
    }

    @GetMapping("/generate")
    public String noteListerGenerate(Model model) {
        log.info("Nu är vi i noteListerGenerate");
        noteListerService.createList();
        return "noteLister";
    }
}
