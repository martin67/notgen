package se.terrassorkestern.notgen2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class InstrumentController {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @RequestMapping("/listInstruments")
    public String listInstruments(Model model) {
        List<Instrument> instruments = instrumentRepository.findByOrderByStandardDescSortOrder();

        model.addAttribute("instruments", instruments);

        return "listInstruments";
    }
}
