package se.terrassorkestern.notgen.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.InitialDataLoader;
import se.terrassorkestern.notgen.user.UserDto;

import javax.validation.Valid;
import java.util.Collections;

@Controller
@RequestMapping("/user")
public class UserController {
    static final Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    public UserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    public String userList(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "userList";

    }

    @GetMapping("/new")
    public String userNew(Model model) {
        model.addAttribute("user", new UserDto());
        return "userEdit";
    }

    @GetMapping("/edit")
    public String userEdit(Model model, @RequestParam(value = "id", required = false) Long id) {

        User u;
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        // If id == null, then it's the user who is editing his own profile
        if (id == null && user.getPrincipal() instanceof User) {
            u = (User)user.getPrincipal();
        } else {
            // First check that the user has permission to edit user (i.e. is admin)
            if (user.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_USER"))) {
                u = userRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("User %d not found", id)));
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
            user.setRoles(Collections.singletonList(roleRepository.findByName("ROLE_USER")));
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
    public String userDelete(@RequestParam("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User %d not found", id)));
        log.info("Tar bort användare " + user.getUsername() + " [" + user.getId() + "]");
        userRepository.delete(user);
        return "redirect:/user/list";
    }

}
