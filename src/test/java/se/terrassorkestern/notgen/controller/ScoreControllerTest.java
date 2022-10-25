package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Score controller")
class ScoreControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private UserRepository userRepository;


    @BeforeEach
    void initTest() {
        Score foo = new Score();
        Score bar = new Score();

        List<Score> allScores = List.of(foo, bar);
        given(scoreRepository.findByOrderByTitle()).willReturn(allScores);
        given(scoreRepository.findById(1)).willReturn(Optional.of(foo));
    }

    @Test
    @DisplayName("List")
    @WithAnonymousUser
    void whenListScores_thenReturnOk() throws Exception {
        mvc.perform(get("/score/list")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("score/list"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_SONG")
    void whenNewScore_thenReturnOk() throws Exception {
        mvc.perform(get("/score/create")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("score/edit"))
                .andExpect(model().attributeExists("score"))
                .andExpect(model().attributeExists("allInstruments"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Edit")
    class Edit {

        @Test
        @DisplayName("Existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenEditValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/score/edit")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "1"))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(model().attributeExists("allInstruments"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/score/edit")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {

        @Test
        @DisplayName("Valid input")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveValidInput_thenReturnRedirect() throws Exception {
            Score score = new Score();
            score.setTitle("My title");
            mvc.perform(post("/score/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .sessionAttr("score", score))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/score/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            Score score = new Score();
            mvc.perform(post("/score/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .sessionAttr("score", score))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Add row")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveAddRow_thenReturnOk() throws Exception {
            mvc.perform(post("/score/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("addRow", "dummy"))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete row")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            mvc.perform(post("/score/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("deleteRow", "0"))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/score/save")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenDeleteValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/score/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/score/list*"));
        }

        @Test
        @DisplayName("Non-existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/score/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void whenAccessProtectedContentAsAnonymousUser_redirectToLogin() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/edit?id=1")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/delete?id=1")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/score/save").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isForbidden());
            mvc.perform(get("/score/edit?id=1")).andExpect(status().isForbidden());
            mvc.perform(get("/score/delete?id=1")).andExpect(status().isForbidden());
            mvc.perform(post("/score/save").with(csrf())).andExpect(status().isForbidden());
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isOk());
            mvc.perform(get("/score/edit?id=1")).andExpect(status().isOk());
            mvc.perform(get("/score/delete?id=1")).andExpect(status().isFound());
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }

    }

}
