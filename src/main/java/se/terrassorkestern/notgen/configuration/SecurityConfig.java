package se.terrassorkestern.notgen.configuration;

import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.UserRepositoryUserDetailsService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(UserRepository userRepository, CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService) {
        this.userRepository = userRepository;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserRepositoryUserDetailsService(userRepository);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .shouldFilterAllDispatcherTypes(true)
                        .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/score/list", "/score/view/**").permitAll()
                        .requestMatchers("/score/**").hasAuthority("EDIT_SONG")
                        .requestMatchers("/user/edit/**", "/user/save").authenticated()
                        .requestMatchers("/user/**").hasAuthority("EDIT_USER")
                        .requestMatchers("/print/**").hasAuthority("PRINT_SCORE")
                        .requestMatchers("/band/**").hasAuthority("EDIT_BAND")
                        .requestMatchers("/playlist/list", "/playlist/view/**", "/playlist/createPdf/**").permitAll()
                        .requestMatchers("/playlist/**").hasAuthority("EDIT_PLAYLIST")
                        .requestMatchers("/instrument/**").hasAuthority("EDIT_INSTRUMENT")
                        .requestMatchers("/admin/**", "/actuator/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin((formLogin) -> formLogin
                        .loginPage("/login")
                )
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/")
                )
                .rememberMe((rememberMe) -> rememberMe
                        .userDetailsService(userDetailsService())
                        .key("asdasdsad")
                )
                .oauth2Login((oauth2Login) -> oauth2Login
                        .loginPage("/login")
                        .userInfoEndpoint()
                        .userService(customOAuth2UserService)
                        .oidcUserService(customOidcUserService)
                );

        return http.build();
    }
}
