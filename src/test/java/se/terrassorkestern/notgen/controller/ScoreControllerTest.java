package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;
import se.terrassorkestern.notgen.service.StorageService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(ScoreController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Score controller")
class ScoreControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private ConverterService converterService;
    @MockBean
    private SongOcrService songOcrService;
    @MockBean
    private StorageService storageService;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Score foo;
    private Score bar;

    @BeforeEach
    void initTest() {
        Band band = new Band();
        foo = new Score();
        bar = new Score();
        foo.setTitle("Foo score");
        bar.setTitle("Bar score");
        Arrangement arr = new Arrangement();
        arr.addArrangementPart(new ArrangementPart(arr, new Instrument()));
        foo.getArrangements().add(arr);
        foo.setDefaultArrangement(arr);

        List<Score> allScores = List.of(foo, bar);
        given(activeBand.getBand()).willReturn(band);
        given(scoreRepository.findByOrderByTitle()).willReturn(allScores);
        given(scoreRepository.findByBandOrderByTitleAsc(band)).willReturn(allScores);
        given(scoreRepository.findById(foo.getId())).willReturn(Optional.of(foo));
        given(scoreRepository.findByBandAndId(band, foo.getId())).willReturn(Optional.of(foo));
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
                .andExpect(model().attributeExists("instruments"))
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
                            .param("id", foo.getId().toString()))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(model().attributeExists("instruments"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/score/edit")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", UUID.randomUUID().toString()))
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
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", foo)
                            .param("save", "dummy")
                            .param("defaultArrangementIndex", "0"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/score/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", new Score())
                            .param("save", "dummy")
                            .param("defaultArrangementIndex", "0"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Add arrangement")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenAddArrangement_thenReturnOk() throws Exception {
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", foo)
                            .param("addArrangement", "dummy"))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete arrangement")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenDeleteArrangement_thenReturnOk() throws Exception {
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", foo)
                            .param("deleteArrangement", foo.getDefaultArrangement().getId().toString()))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Add arrangement part")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveAddRow_thenReturnOk() throws Exception {
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", foo)
                            .param("addArrangementPart", foo.getDefaultArrangement().getId().toString()))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete arrangement part")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            mvc.perform(post("/score/submit").with(csrf())
                            .sessionAttr("score", foo)
                            .param("deleteArrangementPart", foo.getDefaultArrangement().getId().toString() + "-0"))
                    .andExpect(view().name("score/edit"))
                    .andExpect(model().attributeExists("score"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/score/save"))
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
                            .param("id", foo.getId().toString()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/score/list*"));
        }

        @Test
        @DisplayName("Non-existing score")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/score/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", UUID.randomUUID().toString()))
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
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/score/save").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/score/nonexistent")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isForbidden());
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isForbidden());
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(status().isForbidden());
            mvc.perform(post("/score/save").with(csrf())).andExpect(status().isForbidden());
            mvc.perform(get("/score/nonexistent")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_SONG")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/score/list")).andExpect(status().isOk());
            mvc.perform(get("/score/view").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/create")).andExpect(status().isOk());
            mvc.perform(get("/score/edit").param("id", foo.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/score/delete").param("id", foo.getId().toString())).andExpect(redirectedUrl("/score/list"));
            mvc.perform(post("/score/submit")
                            .sessionAttr("score", foo)
                            .with(csrf())
                            .param("defaultArrangementIndex", "0")
                            .param("save", "dummy"))
                    .andExpect(redirectedUrl("/score/list"));
            mvc.perform(get("/score/nonexistent")).andExpect(status().isNotFound());
        }
    }
}
