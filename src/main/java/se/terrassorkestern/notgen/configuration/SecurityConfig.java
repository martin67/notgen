package se.terrassorkestern.notgen.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.UserRepositoryUserDetailsService;

@Slf4j
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
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
                .authorizeRequests()
                .antMatchers("/instrument/**").hasAuthority("EDIT_INSTRUMENT")
                .antMatchers("/score/new/**", "/score/delete/**", "/score/save/**").hasAuthority("EDIT_SONG")
                .antMatchers("/user/new/**", "/user/delete/**", "/user/list/**").hasAuthority("EDIT_USER")
                .antMatchers("/print/**").hasAuthority("PRINT_SCORE")
                .antMatchers("/organization/**").hasAuthority("EDIT_ORGANIZATION")
                .antMatchers("/user/edit/**", "/user/save/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/playlist/new/**", "/playlist/delete/**", "/playlist/copy/**").hasAuthority("EDIT_PLAYLIST")
                .antMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                .antMatchers("/", "/**").permitAll()
                .and()
                .formLogin()
                .failureHandler((request, response, exception) -> log.error("Login error", exception))
                .and()
                .logout()
                .logoutSuccessUrl("/");
        return http.build();
    }

//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> web.debug(false)
//                .ignoring()
//                .antMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico");
//    }

}
