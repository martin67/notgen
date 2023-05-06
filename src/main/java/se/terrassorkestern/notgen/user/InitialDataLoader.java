package se.terrassorkestern.notgen.user;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class InitialDataLoader implements ApplicationListener<ContextRefreshedEvent> {
    public static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_GUEST = "ROLE_GUEST";
    public static final String PRIVILEGE_EDIT_BAND = "EDIT_BAND";
    public static final String PRIVILEGE_EDIT_SONG = "EDIT_SONG";
    public static final String PRIVILEGE_EDIT_INSTRUMENT = "EDIT_INSTRUMENT";
    public static final String PRIVILEGE_EDIT_USER = "EDIT_USER";
    public static final String PRIVILEGE_PRINT_SCORE = "PRINT_SCORE";
    public static final String PRIVILEGE_EDIT_PLAYLIST = "EDIT_PLAYLIST";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final BandRepository bandRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path configPath;

    public InitialDataLoader(UserRepository userRepository, RoleRepository roleRepository,
                             PrivilegeRepository privilegeRepository, BandRepository bandRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${se.terrassorkestern.notgen.storage.content}") String configFolder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
        this.bandRepository = bandRepository;
        this.passwordEncoder = passwordEncoder;
        this.configPath = Path.of(configFolder);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            log.info("Creating initial user admin with admin rights");

            Role superAdmin = createRoleIfNotFound(ROLE_SUPERADMIN, "Super admin", List.of(
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_BAND),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_SONG),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_INSTRUMENT),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_USER),
                    createPrivilegeIfNotFound(PRIVILEGE_PRINT_SCORE),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_PLAYLIST)));
            createRoleIfNotFound(ROLE_ADMIN, "Admin", List.of(
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_SONG),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_INSTRUMENT),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_USER),
                    createPrivilegeIfNotFound(PRIVILEGE_PRINT_SCORE),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_PLAYLIST)));
            createRoleIfNotFound(ROLE_USER, "User", List.of(
                    createPrivilegeIfNotFound(PRIVILEGE_PRINT_SCORE),
                    createPrivilegeIfNotFound(PRIVILEGE_EDIT_PLAYLIST)));
            createRoleIfNotFound(ROLE_GUEST, "Guest", List.of());

            User u = new User();
            u.setUsername("admin");
            u.setFullName("Administrator");
            u.setDisplayName("The Admin");
            u.setEmail("admin@admin");
            u.setPassword(passwordEncoder.encode("password"));
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