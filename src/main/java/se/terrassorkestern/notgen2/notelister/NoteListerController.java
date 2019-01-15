package se.terrassorkestern.notgen2.notelister;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class NoteListerController {

  @Autowired
  NoteListerService noteListerService;


  public NoteListerController() {
    log.debug("Constructor");
  }


  @GetMapping("/noteLister")
  public String noteLister(Model model) {
    log.info("Nu är vi i noteLister(Model model)");

    return "noteLister";
  }

  @GetMapping("/noteListerGenerate")
  public String noteListerGenerate(Model model,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("UPDATE_TOC"))) {
      log.info("Nu är vi i noteListerGenerate");
      noteListerService.createList();
    }
    return "noteLister";
  }
}
