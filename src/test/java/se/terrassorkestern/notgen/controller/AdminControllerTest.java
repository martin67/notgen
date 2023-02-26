package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.AdminService;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.ImageDataExtractor;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(AdminController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Admin controller")
class AdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConverterService converterService;
    @MockBean
    private ImageDataExtractor imageDataExtractor;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private AdminService adminService;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;


    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void accessToProtected_anonymousUser() throws Exception {
            mvc.perform(get("/admin")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void accessToProtected_normalUser() throws Exception {
            mvc.perform(get("/admin")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(roles = "ADMIN")
        void accessToProtected_adminUser() throws Exception {
            mvc.perform(get("/admin")).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Convert all scores")
    @WithMockUser(roles = "ADMIN")
    void create() throws Exception {
        mvc.perform(get("/admin/noteCreate"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Extract image data")
    @WithMockUser(roles = "ADMIN")
    void extract() throws Exception {
        mvc.perform(get("/admin/imageExtract"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Export database dump")
    @WithMockUser(roles = "ADMIN")
    void export() throws Exception {
        mvc.perform(get("/admin/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"));
    }
}
