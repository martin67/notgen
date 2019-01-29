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
import se.terrassorkestern.notgen2.user.UserDto;
import se.terrassorkestern.notgen2.user.UserRepository;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {


  @GetMapping("/")
  public String admin(Model model) {
    return "admin";
  }
}
