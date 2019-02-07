package se.terrassorkestern.notgen2;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
class HomeController {

    @RequestMapping("/")
    public String home() {
        return "home";
    }
}