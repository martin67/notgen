package se.terrassorkestern.notgen2.notelister;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class NoteListerController {

    @Autowired
    NoteListerService noteListerService;


    public NoteListerController() {
        log.info("Constructor!");
    }


    @GetMapping("/noteLister")
    public String noteLister(Model model) {
        log.info("Nu är vi i noteLister(Model model)");

        return "noteLister";
    }

    @GetMapping("/noteListerGenerate")
    public String noteListerGenerate(Model model) {
        log.info("Nu är vi i noteListerGenerate");

        noteListerService.createList();

        return "noteLister";
    }
}
