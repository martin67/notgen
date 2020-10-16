package se.terrassorkestern.notgen2.instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping("/setting")
public class SettingController {
    static final Logger log = LoggerFactory.getLogger(SettingController.class);

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
            log.info("Tar bort sättning " + setting.getName() + " [" + setting.getId() + "]");
            settingRepository.delete(setting);
        }
        return "redirect:/setting/list";
    }

    @PostMapping("/save")
    public String settingSave(@Valid @ModelAttribute Setting setting, Errors errors) {
        if (errors.hasErrors()) {
            return "settingEdit";
        }
        log.info("Sparar sättning " + setting.getName() + " [" + setting.getId() + "]");
        settingRepository.save(setting);
        return "redirect:/setting/list";
    }

}
