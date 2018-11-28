package se.terrassorkestern.notgen2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class InstrumentController {

    private final Logger log = LoggerFactory.getLogger(InstrumentController.class);

    @Autowired
    private InstrumentRepository instrumentRepository;


    @RequestMapping("/instrument/list")
    public String instrumentList(Model model) {
        model.addAttribute("instruments", instrumentRepository.findByOrderByStandardDescSortOrder());
        return "instrumentList";
    }

    @RequestMapping("/instrument/edit")
    public String instrumentEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("instrument", instrumentRepository.findById(id).get());
        return "instrumentEdit";
    }

    @PostMapping("/instrument/update")
    public String instrumentSave(@ModelAttribute Instrument instrument, BindingResult bindingResult, Model model) {
        log.info("Nu är vi i instrumentSave");
        instrumentRepository.save(instrument);
        return "redirect:/instrument/list";
    }


    @PostMapping("/save")
    public String save(Instrument i) {
        log.info("Nu är vi i instrumentSave2");
        instrumentRepository.save(i);
        return "redirect:/";
    }


    @GetMapping("instrumentFind")
    @ResponseBody
    public Optional<Instrument> findById(Integer id) {
        return instrumentRepository.findById(id);
    }

}
