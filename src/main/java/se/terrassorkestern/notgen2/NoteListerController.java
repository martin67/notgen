package se.terrassorkestern.notgen2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class NoteListerController {

    private final Logger log = LoggerFactory.getLogger(NoteListerController.class);

    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private NoteLister noteLister;

    public NoteListerController() {
        log.info("Constructor!");
    }

    @RequestMapping("/noteLister")
    public String noteLister(Model model) {
        log.info("Nu är vi i noteLister(Model model)");


        //model.addAttribute("instruments", instruments);

        return "noteLister";
    }

    @RequestMapping("/noteListerGenerate")
    public String noteListerGenerate(Model model) {
        log.info("Nu är vi i noteListerGenerate");

        noteLister.createList();

        //model.addAttribute("instruments", instruments);

        return "noteLister";
    }
}
