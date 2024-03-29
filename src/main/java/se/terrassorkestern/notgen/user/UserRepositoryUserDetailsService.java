package se.terrassorkestern.notgen.user;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.repository.UserRepository;

@Service
public class UserRepositoryUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ActiveBand activeBand;

    public UserRepositoryUserDetailsService(UserRepository userRepository, ActiveBand activeBand) {
        this.userRepository = userRepository;
        this.activeBand = activeBand;
    }

    @Override
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email '" + username + "' not found"));
        if (!user.isEnabled()) {
            throw new DisabledException("Your account is disabled. Please contact the band admin.");
        }
        // Todo use the default band for the user
        var band = user.getBands().iterator().next();

        activeBand.setBand(band);
        return UserPrincipal.create(user);
    }

}