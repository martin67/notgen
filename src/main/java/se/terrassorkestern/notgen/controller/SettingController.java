package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;

import javax.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/setting")
public class SettingController {

    private final SettingRepository settingRepository;
    private final InstrumentRepository instrumentRepository;


    public SettingController(SettingRepository settingRepository, InstrumentRepository instrumentRepository) {
        this.settingRepository = settingRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @GetMapping("/list")
    public String settingList(Model model) {
        model.addAttribute("settings", settingRepository.findAll());
        return "settingList";
    }

    @GetMapping("/edit")
    public String settingEdit(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("setting", settingRepository.findById(id).orElse(null));
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        return "settingEdit";
    }

    @GetMapping("/new")
    public String settingNew(Model model) {
        model.addAttribute("setting", new Setting());
        model.addAttribute("allInstruments", instrumentRepository.findAll());
        return "settingEdit";
    }

    @GetMapping("/delete")
    public String settingDelete(@RequestParam("id") Integer id, Model model) {
        Setting setting = settingRepository.findById(id).orElse(null);
        if (setting != null) {
            log.info("Tar bort sättning {} [{}]", setting.getName(), setting.getId());
            settingRepository.delete(setting);
        }
        return "redirect:/setting/list";
    }

    @PostMapping("/save")
    public String settingSave(@Valid @ModelAttribute Setting setting, Errors errors) {
        if (errors.hasErrors()) {
            return "settingEdit";
        }
        log.info("Sparar sättning {} [{}]", setting.getName(), setting.getId());
        settingRepository.save(setting);
        return "redirect:/setting/list";
    }

}
