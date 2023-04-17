package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Playlist;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.PlaylistPdfService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(PlaylistController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("Playlist controller")
class PlaylistControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private PlaylistPdfService playlistPdfService;
    @MockBean
    private ConverterService converterService;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Band band1;

    @BeforeEach
    void initTest() throws IOException {
        band1 = new Band();
        Playlist foo = new Playlist();
        foo.setBand(band1);
        Playlist bar = new Playlist();
        bar.setBand(band1);
        Instrument sax = new Instrument();
        sax.setBand(band1);
        List<Playlist> allPlaylists = List.of(foo, bar);

        given(activeBand.getBand()).willReturn(band1);
        given(playlistRepository.findByBandOrderByDateDesc(band1)).willReturn(allPlaylists);
        given(playlistRepository.findByBandAndId(band1, 1)).willReturn(Optional.of(foo));
        given(instrumentRepository.findByBandAndId(band1, sax.getId())).willReturn(Optional.of(sax));
        given(playlistPdfService.create(foo)).willReturn(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    @DisplayName("List")
    @WithAnonymousUser
    void whenListPlaylists_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/list")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("playlist/list"))
                .andExpect(model().attributeExists("playlists"))
                .andExpect(model().attribute("playlists", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_PLAYLIST")
    void whenNewPlaylist_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/create")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("playlist/edit"))
                .andExpect(model().attributeExists("playlist"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create PDF")
    @WithAnonymousUser
    void whenCreatePdf_thenReturnPdf() throws Exception {
        mvc.perform(get("/playlist/createPdf")
                        .contentType(MediaType.TEXT_HTML)
                        .param("id", "1"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Edit")
    class Edit {

        @Test
        @DisplayName("Existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenEditValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/playlist/edit")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "1"))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(model().attributeExists("instruments"))
                    .andExpect(model().attributeExists("settings"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/edit")
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
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveAll_thenReturnRedirect() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("name", "Playlist 1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("id", "1"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Add row")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveAddRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("addRow", "true"))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete row")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("deleteRow", "true"))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @Disabled
        @DisplayName("Create PDF pack")
        @WithAnonymousUser
        void whenSaveCreatePack_thenReturnPdf() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("createPack", "true")
                            .param("selectedInstrument", "1"))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/playlist/save")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/playlist/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/delete")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Copy")
    class Copy {

        @Test
        @DisplayName("Existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/playlist/copy")
                            .contentType(MediaType.TEXT_HTML)
                            .param("id", "1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/copy")
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
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/edit?id=1")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/delete")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/copy")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/playlist/save")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/nonexistent")).andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/edit?id=1")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/delete?id=1")).andExpect(status().isForbidden());
            mvc.perform(post("/playlist/save")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/copy")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/view?id=1")).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isOk());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isNotFound());
        }

    }

}
