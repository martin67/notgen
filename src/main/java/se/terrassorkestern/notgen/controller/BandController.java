package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.repository.BandRepository;

import java.util.UUID;


@Slf4j
@Controller
@RequestMapping("/band")
@SessionAttributes("band")
@PreAuthorize("hasAuthority('EDIT_BAND')")
public class BandController {
    public static final String VIEW_BAND_LIST = "band/list";
    public static final String VIEW_BAND_EDIT = "band/edit";
    public static final String REDIRECT_BAND_LIST = "redirect:/band/list";
    private final BandRepository bandRepository;


    public BandController(BandRepository bandRepository) {
        this.bandRepository = bandRepository;
    }

    @GetMapping({"", "/list"})
    public String list(Model model) {
        model.addAttribute("bands", bandRepository.findAll());
        return VIEW_BAND_LIST;
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") UUID id, Model model) {
        var band = bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Band %s not found", id)));
        model.addAttribute("band", band);
        return VIEW_BAND_EDIT;
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("band", new Band());
        return VIEW_BAND_EDIT;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") UUID id, SessionStatus sessionStatus) {
        var band = bandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Band %s not found", id)));
        log.info("Tar bort band {} [{}]", band.getName(), band.getId());
        bandRepository.delete(band);
        sessionStatus.setComplete();
        return REDIRECT_BAND_LIST;
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Band band, Errors errors, SessionStatus sessionStatus) {
        if (errors.hasErrors()) {
            return VIEW_BAND_EDIT;
        }
        log.info("Sparar band {} [{}]", band.getName(), band.getId());
        bandRepository.save(band);
        sessionStatus.setComplete();
        return REDIRECT_BAND_LIST;
    }

}
