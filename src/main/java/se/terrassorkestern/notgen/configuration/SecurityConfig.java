package se.terrassorkestern.notgen.configuration;

import jakarta.servlet.DispatcherType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
    public AuthenticationManager authManager(HttpSecurity http, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService)
            throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
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
/*
                .antMatchers("/instrument/**").hasAuthority("EDIT_INSTRUMENT")
                .antMatchers("/score/list", "/score/view/**").permitAll()
                .antMatchers("/score/**").hasAuthority("EDIT_SONG")
                .antMatchers("/user/edit/**", "/user/save").authenticated()
                .antMatchers("/user/**").hasAuthority("EDIT_USER")
                .antMatchers("/print/**").hasAuthority("PRINT_SCORE")
                .antMatchers("/organization/**").hasAuthority("EDIT_ORGANIZATION")
                .antMatchers("/playlist/list", "/playlist/view/**", "/playlist/createPdf/**").permitAll()
                .antMatchers("/playlist/**").hasAuthority("EDIT_PLAYLIST")
                .antMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                .antMatchers("/", "/**").permitAll()
                .and()
*/
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
