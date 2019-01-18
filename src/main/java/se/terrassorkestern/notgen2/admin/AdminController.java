package se.terrassorkestern.notgen2.admin;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.user.User;
import se.terrassorkestern.notgen2.user.UserRepository;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {

  @Autowired
  private UserRepository userRepository;

  @GetMapping("")
  public String admin(Model model) {
    model.addAttribute("users", userRepository.findAll());
    return "admin";
  }

  @GetMapping("/userEdit")
  public String userEdit(@RequestParam("id") Long id, Model model) {
    model.addAttribute("user", userRepository.findById(id).get());
    return "userEdit";
  }
  
  @GetMapping("/userNew")
  public String userNew(Model model) {
    model.addAttribute("user", new User());
    return "userEdit";
  }

  @GetMapping("/userDelete")
  public String userDelete(@RequestParam("id") Long id, Model model) {
    User user = userRepository.findById(id).get();
    log.info("Tar bort användare " + user.getUsername() + " [" + user.getId() + "]");
    userRepository.delete(user);
    return "redirect:/admin";
  }
  
  

}
