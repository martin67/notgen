package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
public class SongController {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;


    @ModelAttribute("allInstruments")
    public List<Instrument> populateInstruments() {
        return this.instrumentRepository.findAll();
    }


    @GetMapping("/song/list")
    public String songList(Model model) {
        model.addAttribute("songs", songRepository.findByOrderByTitle());
        return "songList";
    }

    @GetMapping("/song/edit")
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
