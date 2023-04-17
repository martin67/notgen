package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.SettingRepository;

import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/setting")
public class SettingController extends CommonController {

    private final ActiveBand activeBand;
    private final SettingRepository settingRepository;

    public SettingController(ActiveBand activeBand, SettingRepository settingRepository) {
        this.activeBand = activeBand;
        this.settingRepository = settingRepository;
    }

    @GetMapping("/list")
    public String settingList(Model model) {
        model.addAttribute("settings", getSettings());
        return "settingList";
    }

    @GetMapping("/edit")
    public String settingEdit(@RequestParam("id") UUID id, Model model) {
        model.addAttribute("setting", getSetting(id));
        model.addAttribute("allSettings", getSettings());
        return "settingEdit";
    }

    @GetMapping("/new")
    public String settingNew(Model model) {
        model.addAttribute("setting", new Setting());
        model.addAttribute("allSettings", getSettings());
        return "settingEdit";
    }

    @GetMapping("/delete")
    public String settingDelete(@RequestParam("id") UUID id) {
        Setting setting = getSetting(id);
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
