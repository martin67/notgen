package se.terrassorkestern.notgen2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
public class InstrumentController {

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
