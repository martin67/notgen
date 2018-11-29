package se.terrassorkestern.notgen2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SongController {

    private final Logger log = LoggerFactory.getLogger(SongController.class);

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;


    @ModelAttribute("allInstruments")
    public List<Instrument> populateInstruments() {
        return this.instrumentRepository.findAll();
    }


    @RequestMapping("/song/list")
    public String songList(Model model) {
        model.addAttribute("songs", songRepository.findByOrderByTitle());
        return "songList";
    }

    @RequestMapping("/song/edit")
    public String songEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("song", songRepository.findById(id).get());
        return "songEdit";
    }

    @PostMapping("/song/update")
    public String songSave(@ModelAttribute Song song, BindingResult bindingResult, Model model) {
        log.info("Nu är vi i songSave");
        songRepository.save(song);
        return "redirect:/song/list";
    }

}
