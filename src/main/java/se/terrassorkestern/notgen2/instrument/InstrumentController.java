package se.terrassorkestern.notgen2.instrument;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/instrument")
public class InstrumentController {

    @Autowired
    private InstrumentRepository instrumentRepository;


    @GetMapping("/list")
    public String instrumentList(Model model) {
        model.addAttribute("instruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        return "instrumentList";
    }

    @GetMapping("/edit")
    public String instrumentEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("instrument", instrumentRepository.findById(id).get());
        return "instrumentEdit";
    }

    @GetMapping("/new")
    public String instrumentNew(Model model) {
        model.addAttribute("instrument", new Instrument());
        return "instrumentEdit";
    }

    @GetMapping("/delete")
    public String instrumentDelete(@RequestParam("id") Integer id, Model model) {
        Instrument instrument = instrumentRepository.findById(id).get();
        log.info("Tar bort instrument " + instrument.getName() + " [" + instrument.getId() + "]");
        instrumentRepository.delete(instrument);
        return "redirect:/instrument/list";
    }

    @PostMapping("/save")
    public String instrumentSave(@Valid @ModelAttribute Instrument instrument, Errors errors) {
        if (errors.hasErrors()) {
            return "instrumentEdit";
        }
        log.info("Sparar instrument " + instrument.getName() + " [" + instrument.getId() + "]");
        instrumentRepository.save(instrument);
        return "redirect:/instrument/list";
    }

}
