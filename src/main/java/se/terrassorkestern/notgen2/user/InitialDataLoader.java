package se.terrassorkestern.notgen2.user;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InitialDataLoader implements
    ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PrivilegeRepository privilegeRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

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
    user.setRoles(Arrays.asList(adminRole));
    user.setEnabled(true);
    userRepository.save(user);
  }


  @Transactional
  private Privilege createPrivilegeIfNotFound(String name) {

    Privilege privilege = privilegeRepository.findByName(name);
    if (privilege == null) {
      privilege = new Privilege(name);
      privilegeRepository.save(privilege);
    }
    return privilege;
  }

  
  @Transactional
  private Role createRoleIfNotFound(
      String name, Collection<Privilege> privileges) {

    Role role = roleRepository.findByName(name);
    if (role == null) {
      role = new Role(name);
      role.setPrivileges(privileges);
      roleRepository.save(role);
    }
    return role;
  }
}