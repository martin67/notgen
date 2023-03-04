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
import se.terrassorkestern.notgen.repository.SettingRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/setting")
public class SettingController extends CommonController {

    private final ActiveBand activeBand;
    private final SettingRepository settingRepository;
    private final SettingRepository SettingRepository;

    public SettingController(ActiveBand activeBand, SettingRepository settingRepository,
                             SettingRepository SettingRepository) {
        this.activeBand = activeBand;
        this.settingRepository = settingRepository;
        this.SettingRepository = SettingRepository;
    }

    @GetMapping("/list")
    public String settingList(Model model) {
        model.addAttribute("settings", getSettings());
        return "settingList";
    }

    @GetMapping("/edit")
    public String settingEdit(@RequestParam("id") Integer id, Model model) {
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
    public String settingDelete(@RequestParam("id") Integer id) {
        Setting setting = getSetting(id).orElseThrow();
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


    private Optional<Setting> getSetting(int id) {
        Setting setting;
        if (isSuperAdmin()) {
            setting = SettingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %d not found", id)));
        } else {
            setting = SettingRepository.findByIdAndBand(id, activeBand.getBand())
                    .orElseThrow(() -> new NotFoundException(String.format("Setting %d not found", id)));
        }
        return Optional.of(setting);
    }

    private List<Setting> getSettings() {
        List<Setting> settings;
        if (isSuperAdmin()) {
            settings = SettingRepository.findAll();
        } else {
            settings = SettingRepository.findByBand(activeBand.getBand());
        }
        return settings;
    }
}
