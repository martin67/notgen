package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.repository.BandRepository;


@Slf4j
@Controller
@RequestMapping("/band")
public class BandController {
    private final BandRepository bandRepository;


    public BandController(BandRepository bandRepository) {
        this.bandRepository = bandRepository;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("bands", bandRepository.findAll());
        return "bandList";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") Integer id, Model model) {
        Band band = bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Band %d not found", id)));
        model.addAttribute("band", band);
        return "bandEdit";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("band", new Band());
        return "bandEdit";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Integer id) {
        Band band = bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Band %d not found", id)));
        log.info("Tar bort band {} [{}]", band.getName(), band.getId());
        bandRepository.delete(band);
        return "redirect:/bandlist";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Band band, Errors errors) {
        if (errors.hasErrors()) {
            return "bandEdit";
        }
        log.info("Sparar band {} [{}]", band.getName(), band.getId());
        bandRepository.save(band);
        return "redirect:/band/list";
    }

}