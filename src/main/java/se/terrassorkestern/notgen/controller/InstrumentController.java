package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.repository.InstrumentRepository;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/instrument")
public class InstrumentController {

    private final ActiveBand activeBand;
    private final InstrumentRepository instrumentRepository;

    public InstrumentController(ActiveBand activeBand, InstrumentRepository instrumentRepository) {
        this.activeBand = activeBand;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("instruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "instrument/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Integer id, Model model) {
        Instrument instrument = instrumentRepository.findByIdAndBand(id, activeBand.getBand())
                .orElseThrow(() -> new NotFoundException(String.format("Instrument %d not found", id)));
        model.addAttribute("instrument", instrument);
        return "instrument/view";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") Integer id, Model model) {
        Instrument instrument = instrumentRepository.findByIdAndBand(id, activeBand.getBand())
                .orElseThrow(() -> new NotFoundException(String.format("Instrument %d not found", id)));
        model.addAttribute("instrument", instrument);
        return "instrument/edit";
    }

    @GetMapping("/editOrder")
    public String editOrder(Model model) {
        model.addAttribute("instruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "instrument/editOrder";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("instrument", new Instrument());
        return "instrument/edit";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Integer id) {
        Instrument instrument = instrumentRepository.findByIdAndBand(id, activeBand.getBand())
                .orElseThrow(() -> new NotFoundException(String.format("Instrument %d not found", id)));
        log.info("Tar bort instrument {} [{}]", instrument.getName(), instrument.getId());
        instrumentRepository.delete(instrument);
        return "redirect:/instrument/list";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Instrument instrument, Errors errors) {
        if (errors.hasErrors()) {
            return "instrument/edit";
        }
        log.info("Sparar instrument {} [{}]", instrument.getName(), instrument.getId());
        instrument.setBand(activeBand.getBand());
        instrumentRepository.save(instrument);
        return "redirect:/instrument/list";
    }

    @PostMapping("/saveOrder")
    public String saveOrder(@Valid @ModelAttribute List<Instrument> instruments) {
        log.info("Sparar alla instrument {}", instruments);
        for (Instrument instrument : instruments) {
            log.info("Sparar instrument {}", instrument);
            //instrumentRepository.save(instrument);
        }
        return "redirect:/instrument/list";
    }
}
