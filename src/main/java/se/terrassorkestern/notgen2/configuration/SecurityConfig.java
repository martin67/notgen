package se.terrassorkestern.notgen2.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @SuppressWarnings("WeakerAccess")
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/instrument/**")
                .hasAuthority("EDIT_INSTRUMENT")
                .antMatchers("/song/new/**", "/song/delete/**", "/song/save/**")
                .hasAuthority("EDIT_SONG")
                .antMatchers("/user/new/**", "/user/delete/**", "/user/list/**")
                .hasAuthority("EDIT_USER")
                .antMatchers("/playlist/new/**", "/playlist/delete/**", "/playlist/copy/**")
                .hasAuthority("EDIT_PLAYLIST")
                .antMatchers("/noteConverter/**")
                .hasAuthority("CONVERT_SCORE")
                .antMatchers("/noteLister/**")
                .hasAuthority("UPDATE_TOC")
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
