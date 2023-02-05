package se.terrassorkestern.notgen.user;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Privilege;
import se.terrassorkestern.notgen.model.Role;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.BandRepository;
import se.terrassorkestern.notgen.repository.PrivilegeRepository;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
public class InitialDataLoader implements ApplicationListener<ContextRefreshedEvent> {
    static final Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final BandRepository bandRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path configPath;

    public InitialDataLoader(UserRepository userRepository, RoleRepository roleRepository,
                             PrivilegeRepository privilegeRepository, BandRepository bandRepository,
                             PasswordEncoder passwordEncoder, @Value("${notgen.folders.static}") String configFolder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.bandRepository = bandRepository;
        this.passwordEncoder = passwordEncoder;
        this.configPath = Path.of(configFolder);
    }

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {

        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            log.info("Creating initial user admin with admin rights");

            Role superAdmin = createRoleIfNotFound("ROLE_SUPERADMIN", "Super admin", List.of(
                    createPrivilegeIfNotFound("EDIT_BAND"),
                    createPrivilegeIfNotFound("EDIT_SONG"),
                    createPrivilegeIfNotFound("EDIT_INSTRUMENT"),
                    createPrivilegeIfNotFound("EDIT_USER"),
                    createPrivilegeIfNotFound("PRINT_SCORE"),
                    createPrivilegeIfNotFound("EDIT_PLAYLIST")));
            createRoleIfNotFound("ROLE_ADMIN", "Admin", List.of(
                    createPrivilegeIfNotFound("EDIT_SONG"),
                    createPrivilegeIfNotFound("EDIT_INSTRUMENT"),
                    createPrivilegeIfNotFound("EDIT_USER"),
                    createPrivilegeIfNotFound("PRINT_SCORE"),
                    createPrivilegeIfNotFound("EDIT_PLAYLIST")));
            createRoleIfNotFound("ROLE_USER", "User", List.of(
                    createPrivilegeIfNotFound("PRINT_SCORE"),
                    createPrivilegeIfNotFound("EDIT_PLAYLIST")));
            createRoleIfNotFound("ROLE_GUEST", "Guest", List.of());

            User u = new User();
            u.setUsername("admin");
            u.setFullName("Administrator");
            u.setDisplayName("The Admin");
            u.setPassword(passwordEncoder.encode("admin"));
            u.setRole(superAdmin);
            u.setEnabled(true);
            u.setProvider(AuthProvider.local);
            userRepository.save(u);
            return u;
        });

        if (bandRepository.findByName("Terrassorkestern").isEmpty()) {
            log.info("Creating initial band");
            Band band = new Band();
            band.setName("Terrassorkestern");
            bandRepository.save(band);

            adminUser.getBands().add(band);
            userRepository.save(adminUser);
        }

        // Create config directory
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Privilege createPrivilegeIfNotFound(String name) {
        Privilege privilege = privilegeRepository.findByName(name);
        if (privilege == null) {
            privilege = new Privilege(name);
            privilegeRepository.save(privilege);
        }
        return privilege;
    }

    private Role createRoleIfNotFound(String name, String displayName, Collection<Privilege> privileges) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role(name, displayName);
            role.setPrivileges(privileges);
            roleRepository.save(role);
        }
        return role;
    }
}