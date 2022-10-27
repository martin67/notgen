package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.Organization;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.OrganizationRepository;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.UserDto;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;


    public UserController(UserRepository userRepository, RoleRepository roleRepository,
                          OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "user/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        List<Organization> organizations = organizationRepository.findAll();
        model.addAttribute("user", new UserDto());
        model.addAttribute("organizations", organizations);
        return "user/edit";
    }

    @GetMapping("/edit")
    public String edit(Model model, @RequestParam(value = "id", required = false) Long id) {

        User u;
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        // If id == null, then it's the user who is editing his own profile
        if (id == null && user.getPrincipal() instanceof User) {
            u = (User) user.getPrincipal();
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
        return "user/edit";
    }


    @PostMapping("/save")
    public String save(Model model, @Valid @ModelAttribute("user") UserDto userDto, Errors errors) {
        if (errors.hasErrors()) {
            return "user/edit";
        }

        User user = userRepository.findByUsername(userDto.getUsername()).orElseGet(() -> {
            User u = new User();
            u.setUsername(userDto.getUsername());
            u.setRoles(List.of(roleRepository.findByName("ROLE_USER")));
            return u;
        });
        user.setFullname(userDto.getFullname());
        user.setEmail(userDto.getEmail());

        log.info("Sparar användare {}", user.getUsername());
        user.setEnabled(true);

        if (userDto.getPassword().length() > 0) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            log.info("Lösenord: {}", userDto.getPassword());
        }
        userRepository.save(user);
        // Also need to save the details to the current running object??
        return "redirect:/admin";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User %d not found", id)));
        log.info("Tar bort användare {} [{}]", user.getUsername(), user.getId());
        userRepository.delete(user);
        return "redirect:/user/list";
    }

}
