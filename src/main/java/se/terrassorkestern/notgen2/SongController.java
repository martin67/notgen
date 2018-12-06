package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/song")
public class SongController {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;


    @GetMapping("/list")
    public String songList(Model model) {
        model.addAttribute("songs", songRepository.findByOrderByTitle());
        return "songList";
    }

    @GetMapping("/delete")
    public String songDelete(@RequestParam("id") Integer id, Model model) {
        Song song = songRepository.findById(id).get();
        log.info("Tar bort låt " + song.getTitle() + " [" + song.getId() + "]");
        songRepository.delete(song);
        return "redirect:/song/list";
    }

    @GetMapping("/edit")
    public String songEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("allInstruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        model.addAttribute("song", songRepository.findById(id).get());
        return "songEdit";
    }

    @GetMapping("/new")
    public String songNew(Model model) {
        Song song = new Song();
        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        for (Instrument instrument : instrumentRepository.findByStandardIsTrueOrderBySortOrder()) {
            song.getScoreParts().add(new ScorePart(song, instrument));
        }
        model.addAttribute("song", song);
        model.addAttribute("allInstruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        return "songEdit";
    }

    @PostMapping("/save")
    public String songSave(@ModelAttribute Song song, BindingResult bindingResult, Model model) {
        log.info("Sparar låt " + song.getTitle() + " [" + song.getId() + "]");
        // scorPart måste fixas till efter formuläret
        for (ScorePart scorePart : song.getScoreParts()) {
            Instrument instrument = scorePart.getInstrument();
            scorePart.setId(new ScorePartId(song.getId(), instrument.getId()));
            scorePart.setSong(song);
            scorePart.setInstrument(instrumentRepository.findById(instrument.getId()).get());
        }
        songRepository.save(song);
        return "redirect:/song/list";
    }

    @PostMapping(value = "/update", params = {"addRow"})
    public String addRow(final Song song, final BindingResult bindingResult, Model model) {
        song.getScoreParts().add(new ScorePart());
        model.addAttribute("allInstruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        model.addAttribute("song", song);
        return "songEdit";
    }

    @PostMapping(value = "/update", params = {"deleteRow"})
    public String deleteRow(final Song song, final BindingResult bindingResult, Model model, final HttpServletRequest req) {
        final Integer scorePartId = Integer.valueOf(req.getParameter("deleteRow"));
        song.getScoreParts().remove(scorePartId.intValue());
        model.addAttribute("allInstruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        model.addAttribute("song", song);
        return "songEdit";
    }

}