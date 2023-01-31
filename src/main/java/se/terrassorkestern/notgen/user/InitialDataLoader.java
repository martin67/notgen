package se.terrassorkestern.notgen.user;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Organization;
import se.terrassorkestern.notgen.model.Privilege;
import se.terrassorkestern.notgen.model.Role;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.OrganizationRepository;
import se.terrassorkestern.notgen.repository.PrivilegeRepository;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Component
public class InitialDataLoader implements ApplicationListener<ContextRefreshedEvent> {
    static final Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path configPath;

    public InitialDataLoader(UserRepository userRepository, RoleRepository roleRepository,
                             PrivilegeRepository privilegeRepository, OrganizationRepository organizationRepository,
                             PasswordEncoder passwordEncoder, @Value("${notgen.folders.static}") String configFolder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.configPath = Path.of(configFolder);
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {

        User user = userRepository.findByUsername("admin").orElseGet(() -> {
            log.info("Creating initial user admin with admin rights");

            createRoleIfNotFound("ROLE_SUPERADMIN", List.of(
                    createPrivilegeIfNotFound("EDIT_ORGANIZATION")));

            createRoleIfNotFound("ROLE_ADMIN", List.of(
                    createPrivilegeIfNotFound("EDIT_SONG"),
                    createPrivilegeIfNotFound("EDIT_INSTRUMENT"),
                    createPrivilegeIfNotFound("EDIT_USER")));

            createRoleIfNotFound("ROLE_USER", List.of(
                    createPrivilegeIfNotFound("PRINT_SCORE"),
                    createPrivilegeIfNotFound("EDIT_PLAYLIST")));

            List<Role> adminRoles = roleRepository.findAll();
            User u = new User();
            u.setUsername("admin");
            u.setFullname("Thore Terrass");
            u.setPassword(passwordEncoder.encode("admin"));
            u.setRoles(adminRoles);
            u.setEnabled(true);
            userRepository.save(u);
            return u;
        });

        if (organizationRepository.findByName("Terrassorkestern").isEmpty()) {
            log.info("Creating initial band");
            Organization organization = new Organization();
            organization.setName("Terrassorkestern");
            organizationRepository.save(organization);

            user.setOrganization(organization);
            userRepository.save(user);
        }

        // Create config directory
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    void createRoleIfNotFound(String name, Collection<Privilege> privileges) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
    }
}