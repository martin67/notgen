package se.terrassorkestern.notgen.user;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.terrassorkestern.notgen.exceptions.OAuth2AuthenticationProcessingException;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.User;
import se.terrassorkestern.notgen.repository.RoleRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import java.util.Optional;

@Service
public class CommonOAuth2UserService {

    private final ActiveBand activeBand;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CommonOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository, ActiveBand activeBand) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.activeBand = activeBand;
    }

    UserPrincipal processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        if (!StringUtils.hasLength(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            if (!user.isEnabled()) {
                throw new DisabledException("Your account is disabled. Please contact the band admin.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }
        // Todo use the default band for the user
        Band band = user.getBands().iterator().next();

        activeBand.setBand(band);
        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        User user = new User();

        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFullName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setImageUrl(oAuth2UserInfo.getImageUrl());
        user.setRole(roleRepository.findByName("ROLE_GUEST"));
        // Band will be set by the admin
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setFullName(oAuth2UserInfo.getName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }

}
