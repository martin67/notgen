package se.terrassorkestern.notgen2.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.noteconverter.NoteConverterDto;
import se.terrassorkestern.notgen2.service.NoteConverterService;
import se.terrassorkestern.notgen2.repository.ScoreRepository;
import se.terrassorkestern.notgen2.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteConverterController.class)
@DisplayName("NoteConverter controller")
class NoteConverterControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private NoteConverterService noteConverterService;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private UserRepository userRepository;


    @Nested
    @DisplayName("Convert")
    class Convert {

        @Test
        @WithMockUser(authorities = "CONVERT_SCORE")
        @DisplayName("Set scores")
        void convert_get() throws Exception {
            mvc.perform(get("/noteConverter"))
                    .andExpect(view().name("noteConverter"))
                    .andExpect(model().attributeExists("scores", "noteConverterDto"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(authorities = "CONVERT_SCORE")
        @DisplayName("Convert notes")
        void convert_postNotes() throws Exception {
            mvc.perform(post("/noteConverter/convert").with(csrf())
                    .flashAttr("noteConverterDto", new NoteConverterDto())
                    .param("convertNotes", "dummy"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/noteConverter**"));
        }

        @Test
        @WithMockUser(authorities = "CONVERT_SCORE")
        @DisplayName("Convert packs")
        void convert_postPacks() throws Exception {
            mvc.perform(post("/noteConverter/convert").with(csrf())
                    .flashAttr("noteConverterDto", new NoteConverterDto())
                    .param("createPacks", "dummy"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/noteConverter**"));
        }
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @WithAnonymousUser
        @DisplayName("Anonymous user")
        void accessToProtected_anonymousUser() throws Exception {
            mvc.perform(get("/noteConverter")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/noteConverter/convert").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser
        @DisplayName("Normal user")
        void accessToProtected_normalUser() throws Exception {
            mvc.perform(get("/noteConverter")).andExpect(status().isForbidden());
            mvc.perform(post("/noteConverter/convert").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = "CONVERT_SCORE")
        @DisplayName("Admin user")
        void accessToProtected_adminUser() throws Exception {
            mvc.perform(get("/noteConverter")).andExpect(status().isOk());
        }
    }
}
