package se.terrassorkestern.notgen2.instrument;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/instrument")
public class InstrumentController {

  @Autowired
  private InstrumentRepository instrumentRepository;

  @GetMapping("/list")
  public String instrumentList(Model model) {
    model.addAttribute("instruments", instrumentRepository.findByOrderByStandardDescSortOrder());
    return "instrumentList";
  }

  @GetMapping("/edit")
  public String instrumentEdit(@RequestParam("id") Integer id, Model model) {
    model.addAttribute("instrument", instrumentRepository.findById(id).get());
    return "instrumentEdit";
  }

  @GetMapping("/new")
  public String instrumentNew(Model model) {
    model.addAttribute("instrument", new Instrument());
    return "instrumentEdit";
  }

  @GetMapping("/delete")
  public String instrumentDelete(@RequestParam("id") Integer id, Model model,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_INSTRUMENT"))) {
      Instrument instrument = instrumentRepository.findById(id).get();
      log.info("Tar bort instrument " + instrument.getName() + " [" + instrument.getId() + "]");
      instrumentRepository.delete(instrument);
    }
    return "redirect:/instrument/list";
  }

  @PostMapping("/save")
  public String instrumentSave(@Valid @ModelAttribute Instrument instrument, Errors errors,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (errors.hasErrors()) {
      return "instrumentEdit";
    }
    if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_INSTRUMENT"))) {
      log.info("Sparar instrument " + instrument.getName() + " [" + instrument.getId() + "]");
      instrumentRepository.save(instrument);
    }
    return "redirect:/instrument/list";
  }

}
