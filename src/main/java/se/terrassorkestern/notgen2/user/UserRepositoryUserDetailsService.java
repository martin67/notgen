package se.terrassorkestern.notgen2.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserRepositoryUserDetailsService
    implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username)
      throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username);
    if (user != null) {
      return new org.springframework.security.core.userdetails.User(
          user.getUsername(), user.getPassword(), user.getEnabled(), true, true, 
          true, getAuthorities(user.getRoles()));
    }
    throw new UsernameNotFoundException(
        "User '" + username + "' not found");
  }

  private Collection<? extends GrantedAuthority> getAuthorities(
      Collection<Role> roles) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    for (Role role: roles) {
      authorities.add(new SimpleGrantedAuthority(role.getName()));
      role.getPrivileges().stream()
      .map(p -> new SimpleGrantedAuthority(p.getName()))
      .forEach(authorities::add);
    }

    return authorities;
  }

}