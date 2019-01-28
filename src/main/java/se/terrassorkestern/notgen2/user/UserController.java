package se.terrassorkestern.notgen2.user;

import java.util.Arrays;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {
  
  @Autowired
  private UserRepository userRepository;
  
  @Autowired
  private RoleRepository roleRepository;
  
  @Autowired
  private PasswordEncoder passwordEncoder;
  
  
  @GetMapping("/edit")
  public String userEdit(Model model, @AuthenticationPrincipal User user) {
    // Reset the password
    user.setPassword("");
    model.addAttribute("user", user);
    return "userEdit";
  }
 
  @PostMapping("/save")
  public String userSave(@Valid @ModelAttribute User user, Errors errors) {
    if (errors.hasErrors()) {
      return "userEdit";
    }
    log.info("Sparar användare " + user.getUsername() + " [" + user.getId() + "]");
    user.setEnabled(true);
    
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
    userRepository.save(user);
    // Also need to save the details to the current running object??
    return "userEdit";
  }
}
