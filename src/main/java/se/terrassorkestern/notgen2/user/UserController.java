package se.terrassorkestern.notgen2.user;

import java.util.Arrays;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;


  @GetMapping("/list")
  public String userList(Model model, @AuthenticationPrincipal User user) {
    if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_USER"))) {
      model.addAttribute("users", userRepository.findAll());
      return "userList";
    } else {
      return "redirect:/";
    }
  }

  @GetMapping("/new")
  public String userNew(Model model) {
    model.addAttribute("user", new UserDto());
    return "userEdit";
  }

  @GetMapping("/edit")
  public String userEdit(Model model, @RequestParam(value = "id", required = false) Long id, @AuthenticationPrincipal User user) {

    User u;

    // If id == null, then it's the user who is editing his own profile
    if (id == null) {
      u = user;
    } else {
      // First check that the user has permission to edit user (i.e. is admin)
      if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_USER"))) {
        u = userRepository.findById(id).get();
      } else {
        return "redirect:/";
      }
    }

    UserDto userDto = new UserDto();
    userDto.setUsername(u.getUsername());
    userDto.setFullname(u.getFullname());
    userDto.setEmail(u.getEmail());
    model.addAttribute("user", userDto);
    return "userEdit";
  }


  @PostMapping("/save")
  public String userSave(Model model, @Valid @ModelAttribute("user") UserDto userDto, Errors errors) {
    if (errors.hasErrors()) {
      return "userEdit";
    }

    User user = userRepository.findByUsername(userDto.getUsername());
    if (user == null) {
      user = new User();
      user.setUsername(userDto.getUsername());
      user.setRoles(Arrays.asList(roleRepository.findByName("ROLE_USER")));
    }
    user.setFullname(userDto.getFullname());
    user.setEmail(userDto.getEmail());

    log.info("Sparar användare " + user.getUsername());
    user.setEnabled(true);

    if (userDto.getPassword().length() > 0) { 
      user.setPassword(passwordEncoder.encode(userDto.getPassword()));
      log.info("Lösenord: " + userDto.getPassword());
    }
    userRepository.save(user);
    // Also need to save the details to the current running object??
    return "redirect:/";
  }

  @GetMapping("/delete")
  public String userDelete(@RequestParam("id") Long id, Model model, @AuthenticationPrincipal User user) {
    if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_USER"))) {
      User u = userRepository.findById(id).get();
      log.info("Tar bort användare " + u.getUsername() + " [" + u.getId() + "]");
      userRepository.delete(u);
      return "redirect:/user/list";
    } else {
      return "redirect:/";
    }
  }

}