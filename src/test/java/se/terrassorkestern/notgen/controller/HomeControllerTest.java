package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest
@Import(HomeController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Home controller")
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;


    @Test
    @DisplayName("Home")
    void testHomePage() throws Exception {
        mvc.perform(get("/")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    @DisplayName("About")
    void testAboutPage() throws Exception {
        mvc.perform(get("/about")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("about"));
    }

}