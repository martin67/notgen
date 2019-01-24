package se.terrassorkestern.notgen2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Notgen2Application {

  public static void main(String[] args) {
    SpringApplication.run(Notgen2Application.class, args);
  }

  /*
   * @Configuration public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter
   * {
   * 
   * @Override protected void configure(HttpSecurity http) throws Exception {
   * http.authorizeRequests().anyRequest().permitAll() .and().csrf().disable(); } }
   */
  
}
