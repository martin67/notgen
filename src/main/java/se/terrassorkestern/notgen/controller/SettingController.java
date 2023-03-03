package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;

@Slf4j
@Controller
@RequestMapping("/setting")
public class SettingController {

    private final ActiveBand activeBand;
    private final SettingRepository settingRepository;
    private final InstrumentRepository instrumentRepository;

    public SettingController(ActiveBand activeBand, SettingRepository settingRepository,
                             InstrumentRepository instrumentRepository) {
        this.activeBand = activeBand;
        this.settingRepository = settingRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/list")
    public String settingList(Model model) {
        model.addAttribute("settings", settingRepository.findByBand(activeBand.getBand()));
        return "settingList";
    }

    @GetMapping("/edit")
    public String settingEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("setting", settingRepository.findByIdAndBand(id, activeBand.getBand()).orElseThrow(
                () -> new NotFoundException(String.format("Setting %d not found", id))));
        model.addAttribute("allInstruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "settingEdit";
    }

    @GetMapping("/new")
    public String settingNew(Model model) {
        model.addAttribute("setting", new Setting());
        model.addAttribute("allInstruments", instrumentRepository.findByBandOrderBySortOrder(activeBand.getBand()));
        return "settingEdit";
    }

    @GetMapping("/delete")
    public String settingDelete(@RequestParam("id") Integer id) {
        Setting setting = settingRepository.findByIdAndBand(id, activeBand.getBand()).orElseThrow(
                () -> new NotFoundException(String.format("Setting %d not found", id)));
        log.info("Tar bort sättning {} [{}]", setting.getName(), setting.getId());
        settingRepository.delete(setting);
        return "redirect:/setting/list";
    }

    @PostMapping("/save")
    public String settingSave(@Valid @ModelAttribute Setting setting, Errors errors) {
        if (errors.hasErrors()) {
            return "settingEdit";
        }
        log.info("Sparar sättning {} [{}]", setting.getName(), setting.getId());
        setting.setBand(activeBand.getBand());
        settingRepository.save(setting);
        return "redirect:/setting/list";
    }

}
