package se.terrassorkestern.notgen2.user;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen2.song.SongController;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {
  
  @Autowired
  private UserRepository userRepository;
  
  
  @GetMapping("/edit")
  public String userEdit(Model model, @AuthenticationPrincipal User user) {
    model.addAttribute("user", user);
    return "userEdit";
  }
 
  @PostMapping("/save")
  public String userSave(@Valid @ModelAttribute User user, Errors errors) {
    if (errors.hasErrors()) {
      return "userEdit";
    }
    log.info("Sparar användare " + user.getUsername() + " [" + user.getId() + "]");
    userRepository.save(user);
    // Also need to save the details to the current running object??
    return "userEdit";
  }
}
