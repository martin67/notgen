package se.terrassorkestern.notgen.configuration;

import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.UserRepositoryUserDetailsService;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                        .requestMatchers("/organization/**").hasAuthority("EDIT_ORGANIZATION")
                        .requestMatchers("/playlist/list", "/playlist/view/**", "/playlist/createPdf/**").permitAll()
                        .requestMatchers("/playlist/**").hasAuthority("EDIT_PLAYLIST")
                        .requestMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin()
                //.failureHandler((request, response, exception) -> log.error("Login error", exception))
                .and()
                .logout()
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/")
                .and()
                .rememberMe()
                .userDetailsService(userDetailsService())
                .key("jaksdladnsdasd");
        return http.build();
    }
}
