package se.terrassorkestern.notgen.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Privilege;
import se.terrassorkestern.notgen.model.Role;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.PrivilegeRepository;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class InitialDataLoader implements ApplicationListener<ContextRefreshedEvent> {
    static final Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final PasswordEncoder passwordEncoder;


    public InitialDataLoader(UserRepository userRepository, RoleRepository roleRepository,
                             PrivilegeRepository privilegeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (userRepository.findByUsername("admin") != null) {
            return;
        }
        log.info("Creating initial user admin with admin rights");

        List<Privilege> adminPrivileges = Arrays.asList(
                createPrivilegeIfNotFound("PRINT_SCORE"),
                createPrivilegeIfNotFound("EDIT_SONG"),
                createPrivilegeIfNotFound("EDIT_INSTRUMENT"),
                createPrivilegeIfNotFound("EDIT_PLAYLIST"),
                createPrivilegeIfNotFound("EDIT_USER"),
                createPrivilegeIfNotFound("CONVERT_SCORE"),
                createPrivilegeIfNotFound("UPDATE_TOC"));

        List<Privilege> userPrivileges = Arrays.asList(
                createPrivilegeIfNotFound("PRINT_SCORE"),
                createPrivilegeIfNotFound("EDIT_PLAYLIST"));

        createRoleIfNotFound("ROLE_ADMIN", adminPrivileges);
        createRoleIfNotFound("ROLE_USER", userPrivileges);

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        User user = new User();
        user.setUsername("admin");
        user.setFullname("Thore Terrass");
        user.setPassword(passwordEncoder.encode("plettLagg"));
        user.setRoles(Collections.singletonList(adminRole));
        user.setEnabled(true);
        userRepository.save(user);
    }


    @Transactional
    Privilege createPrivilegeIfNotFound(String name) {

        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }


    @Transactional
    void createRoleIfNotFound(
            String name, Collection<Privilege> privileges) {

        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
    }
}