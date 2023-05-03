package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.repository.InstrumentRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/instrument")
public class InstrumentController extends CommonController {

    public static final String ATTRIBUTE_ALL_INSTRUMENTS = "instruments";
    public static final String ATTRIBUTE_ONE_INSTRUMENT = "instrument";
    public static final String REDIRECT_INSTRUMENT_LIST = "redirect:/instrument/list";
    public static final String VIEW_INSTRUMENT_EDIT = "instrument/edit";
    public static final String VIEW_INSTRUMENT_VIEW = "instrument/view";
    public static final String VIEW_INSTRUMENT_LIST = "instrument/list";
    private final ActiveBand activeBand;
    private final InstrumentRepository instrumentRepository;

    public InstrumentController(ActiveBand activeBand, InstrumentRepository instrumentRepository) {
        this.activeBand = activeBand;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        return VIEW_INSTRUMENT_LIST;
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") UUID id, Model model) {
        model.addAttribute(ATTRIBUTE_ONE_INSTRUMENT, getInstrument(id));
        return VIEW_INSTRUMENT_VIEW;
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") UUID id, Model model) {
        model.addAttribute(ATTRIBUTE_ONE_INSTRUMENT, getInstrument(id));
        return VIEW_INSTRUMENT_EDIT;
    }

    @GetMapping("/editOrder")
    public String editOrder(Model model) {
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        return "instrument/editOrder";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute(ATTRIBUTE_ONE_INSTRUMENT, new Instrument());
        return VIEW_INSTRUMENT_EDIT;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") UUID id) {
        Instrument instrument = getInstrument(id);
        log.info("Tar bort instrument {} [{}]", instrument.getName(), instrument.getId());
        instrumentRepository.delete(instrument);
        return REDIRECT_INSTRUMENT_LIST;
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Instrument instrument, Errors errors) {
        if (errors.hasErrors()) {
            return VIEW_INSTRUMENT_EDIT;
        }
        log.info("Sparar instrument {} [{}]", instrument.getName(), instrument.getId());
        instrument.setBand(activeBand.getBand());
        instrumentRepository.save(instrument);
        return REDIRECT_INSTRUMENT_LIST;
    }

    @PostMapping("/saveOrder")
    public String saveOrder(@Valid @ModelAttribute List<Instrument> instruments) {
        log.info("Sparar alla instrument {}", instruments);
        for (Instrument instrument : instruments) {
            log.info("Sparar instrument {}", instrument);
            //instrumentRepository.save(instrument);
        }
        return REDIRECT_INSTRUMENT_LIST;
    }

}
