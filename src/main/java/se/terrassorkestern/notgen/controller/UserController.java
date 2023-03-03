package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
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
import se.terrassorkestern.notgen.exceptions.UserAlreadyExistAuthenticationException;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.BandRepository;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.AuthProvider;
import se.terrassorkestern.notgen.user.UserFormData;
import se.terrassorkestern.notgen.user.UserPrincipal;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BandRepository bandRepository;
    private final PasswordEncoder passwordEncoder;


    public UserController(UserRepository userRepository, RoleRepository roleRepository,
                          BandRepository bandRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bandRepository = bandRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    public String list(Model model) {
        List<User> bandUsers = new ArrayList<>();
        List<User> otherUsers = new ArrayList<>();

        // Todo use band from session
        Band band = bandRepository.findAll().get(0);

        for (User user : userRepository.findAll()) {
            if (user.getBands().contains(band)) {
                bandUsers.add(user);
            } else {
                otherUsers.add(user);
            }
        }
        model.addAttribute("bandUsers", bandUsers);
        model.addAttribute("otherUsers", otherUsers);
        return "user/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        List<Band> bands = bandRepository.findAll();
        model.addAttribute("user", new UserFormData());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("bands", bands);
        return "user/edit";
    }

    @GetMapping("/edit")
    public String edit(Model model, @RequestParam(value = "id", defaultValue = "-1") long id) {

        User user;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // If id == null, then it's the user who is editing his own profile
        if (id == -1) {
            user = ((UserPrincipal) authentication.getPrincipal()).getUser();
        } else {
            // First check that the user has permission to edit user (i.e. is admin)
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("EDIT_USER"))) {
                user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("User %d not found", id)));
            } else {
                return "redirect:/";
            }
        }
        UserFormData userFormData = new UserFormData(user);
        model.addAttribute("user", userFormData);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("bands", bandRepository.findAll());
        return "user/edit";
    }


    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("user") UserFormData userFormData, Errors errors) {
        if (errors.hasErrors()) {
            return "user/edit";
        }

        // true if the edit/save is done by an admin user
        boolean adminEdit = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .contains(new SimpleGrantedAuthority("EDIT_USER"));

        User user = userRepository.findById(userFormData.getId()).orElseGet(() -> {
            // new user
            // check that username & email is not used
            if (userRepository.findByUsername(userFormData.getUsername()).isPresent()) {
                throw new UserAlreadyExistAuthenticationException("username " + userFormData.getUsername() + " already exist");
            }
            if (userRepository.findByEmail(userFormData.getEmail()).isPresent()) {
                throw new UserAlreadyExistAuthenticationException("email " + userFormData.getEmail() + " already exist");
            }
            User u = new User();
            u.setUsername(userFormData.getUsername());
            u.setEmail(userFormData.getEmail());
            u.setProvider(AuthProvider.local);
            u.setRole(roleRepository.findByName("ROLE_USER"));
            return u;
        });

        user.setDisplayName(userFormData.getDisplayName());

        // Can't set full name, email & password for oauth2 accounts
        if (user.isLocalUser()) {
            user.setFullName(userFormData.getFullName());
            if (!user.getEmail().equals(userFormData.getEmail())) {
                if (userRepository.findByEmail(userFormData.getEmail()).isPresent()) {
                    throw new UserAlreadyExistAuthenticationException("email " + userFormData.getEmail() + " already exist");
                }
                user.setEmail(userFormData.getEmail());
            }

            if (userFormData.getPassword().length() > 0) {
                user.setPassword(passwordEncoder.encode(userFormData.getPassword()));
                log.info("Lösenord: {}", userFormData.getPassword());
            }
        }

        // admin user can change enable status and username (if local user). But not for the superadmin ("admin") account
        if (adminEdit && (user.isRemoteUser() || !user.getUsername().equals("admin"))) {
            user.setEnabled(userFormData.isEnabled());
            user.setBands(userFormData.getBands());
            user.setRole(userFormData.getRole());
            if (user.isLocalUser() && !userFormData.getUsername().equals(user.getUsername())) {
                if (userRepository.findByUsername(userFormData.getUsername()).isPresent()) {
                    throw new UserAlreadyExistAuthenticationException("username " + userFormData.getUsername() + " already exist");
                }
                user.setUsername(userFormData.getUsername());
            }
        }

        log.info("Sparar användare {} ({})", user.getId(), user.getFullName());

        userRepository.save(user);

        // if the user saved is the current user -> update the principal
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.getId() == userPrincipal.getId()) {
            userPrincipal.updateUser(user);
        }
        // If we are an admin editing the user, return to the user list. Otherwise go to the start screen.
        // Or should we go the last page used??
        if (adminEdit) {
            return "redirect:/user/list";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User %d not found", id)));
        log.info("Tar bort användare {} [{}]", user.getUsername(), user.getId());
        userRepository.delete(user);
        return "redirect:/user/list";
    }

}
