package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.service.NoteListerService;
import se.terrassorkestern.notgen.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteListerController.class)
@DisplayName("NoteLister controller")
class NoteListerControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private NoteListerService noteListerService;
    @MockBean
    private UserRepository userRepository;


    @Test
    @WithMockUser(authorities = "UPDATE_TOC")
    @DisplayName("Generate")
    void generate_returnOk() throws Exception {
        mvc.perform(get("/noteLister/generate"))
                .andExpect(view().name("noteLister"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous user")
        void accessToProtected_anonymousUser() throws Exception {
            mvc.perform(get("/noteLister")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser
        @DisplayName("Normal user")
        void accessToProtected_normalUser() throws Exception {
            mvc.perform(get("/noteLister")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "UPDATE_TOC")
        @DisplayName("Admin user")
        void accessToProtected_adminUser() throws Exception {
            mvc.perform(get("/noteLister")).andExpect(status().isOk());
        }
    }

}
