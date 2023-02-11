package se.terrassorkestern.notgen.service;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.UserPrincipal;

@Service
public class UserRepositoryUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserRepositoryUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email '" + username + "' not found"));
        if (!user.isEnabled()) {
            throw new DisabledException("Your account is disabled. Please contact the band admin.");
        }
        return UserPrincipal.create(user);
    }

}