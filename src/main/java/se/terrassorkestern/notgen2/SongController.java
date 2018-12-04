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

@Slf4j
@Controller
public class SongController {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;


    @GetMapping("/song/list")
    public String songList(Model model) {
        model.addAttribute("songs", songRepository.findByOrderByTitle());
        return "songList";
    }

    @GetMapping("/song/edit")
    public String songEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        model.addAttribute("song", songRepository.findById(id).get());
        return "songEdit";
    }

    @PostMapping("/song/update")
    public String songSave(@ModelAttribute Song song, BindingResult bindingResult, Model model) {
        log.info("Nu är vi i songSave");
        // song sätts inte i formuläret
        for (ScorePart scorePart : song.getScoreParts()) {
            scorePart.setSong(song);
            Instrument instrument = scorePart.getInstrument();
            scorePart.setId(new ScorePartId(song.getId(), instrument.getId()));
            // Det är bara id på instrumentet som kommer med, inte hela instrumentet...
            scorePart.setInstrument(instrumentRepository.findById(instrument.getId()).get());
        }
        songRepository.save(song);
        return "redirect:/song/list";
    }

}
