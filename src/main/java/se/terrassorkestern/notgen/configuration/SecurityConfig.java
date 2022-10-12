package se.terrassorkestern.notgen.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.UserRepositoryUserDetailsService;

@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService myUserDetailsService() {
        return new UserRepositoryUserDetailsService(userRepository);
    }

    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(myUserDetailsService()).passwordEncoder(bcryptPasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/instrument/**")
                .hasAuthority("EDIT_INSTRUMENT")
                .antMatchers("/score/new/**", "/score/delete/**", "/score/save/**")
                .hasAuthority("EDIT_SONG")
                .antMatchers("/user/new/**", "/user/delete/**", "/user/list/**")
                .hasAuthority("EDIT_USER")
                .antMatchers("/user/edit/**", "/user/save/**")
                .hasAnyRole("USER", "ADMIN")
                .antMatchers("/playlist/new/**", "/playlist/delete/**", "/playlist/copy/**")
                .hasAuthority("EDIT_PLAYLIST")
                .antMatchers("/admin/**", "/actuator/**")
                .hasRole("ADMIN")
                .antMatchers("/", "/**").permitAll()
                .and()
                .formLogin()
                .and()
                .logout()
                .logoutSuccessUrl("/")
        ;
    }

}
