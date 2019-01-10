package se.terrassorkestern.notgen2.admin;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import se.terrassorkestern.notgen2.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/admin")
public class AdminController {

  @Autowired
  private UserRepository userRepository;

  @GetMapping("")
  public String admin(Model model) {
    model.addAttribute("users", userRepository.findAll());
    return "admin";
  }

}
