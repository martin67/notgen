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
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.PlaylistPdfService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
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

    private Playlist playlist;
    private Instrument sax;

    @BeforeEach
    void initTest() throws IOException {
        var band = new Band("The band", "The test band");
        sax = new Instrument(band, "Altsax", "asx", 1);
        var setting = new Setting(band, "Test setting");
        setting.getInstruments().add(sax);
        playlist = new Playlist(band, "Test playlist", "Test comment", LocalDate.now(), setting);
        var playlistEntry = new PlaylistEntry(1, "my text", false, "my comment");
        playlist.getPlaylistEntries().add(playlistEntry);

        var bar = new Playlist(band, "Second playlist", "A comment", LocalDate.now(), setting);
        var allPlaylists = List.of(playlist, bar);

        given(activeBand.getBand()).willReturn(band);
        given(playlistRepository.findByBandOrderByDateDesc(band)).willReturn(allPlaylists);
        given(playlistRepository.findByBandAndId(band, playlist.getId())).willReturn(Optional.of(playlist));
        given(instrumentRepository.findByBandAndId(band, sax.getId())).willReturn(Optional.of(sax));
        given(playlistPdfService.create(playlist)).willReturn(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    @DisplayName("List")
    @WithAnonymousUser
    void whenListPlaylists_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/list"))
                .andExpect(view().name("playlist/list"))
                .andExpect(model().attributeExists("playlists"))
                .andExpect(model().attribute("playlists", hasSize(2)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_PLAYLIST")
    void whenNewPlaylist_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/create"))
                .andExpect(view().name("playlist/edit"))
                .andExpect(model().attributeExists("playlist"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create PDF")
    @WithAnonymousUser
    void whenCreatePdf_thenReturnPdf() throws Exception {
        mvc.perform(get("/playlist/createPdf")
                        .param("id", playlist.getId().toString()))
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
                            .param("id", playlist.getId().toString()))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(model().attributeExists("instruments"))
                    .andExpect(model().attributeExists("settings"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/edit")
                            .param("id", UUID.randomUUID().toString()))
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
                            .sessionAttr("playlist", playlist)
                            .param("name", "Playlist 1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            playlist.setName("");
            mvc.perform(post("/playlist/save").with(csrf())
                            .sessionAttr("playlist", playlist))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Add row")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveAddRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .sessionAttr("playlist", playlist)
                            .param("addRow", "dummy"))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete row")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .sessionAttr("playlist", playlist)
                            .param("deleteRow", "0"))
                    .andExpect(view().name("playlist/edit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(status().isOk());
        }

        @Test
        @Disabled
        @DisplayName("Create PDF pack")
        @WithAnonymousUser
        void whenSaveCreatePack_thenReturnPdf() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                            .param("createPack", "true")
                            .param("selectedInstrument", sax.getId().toString()))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/playlist/save"))
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
                            .param("id", playlist.getId().toString()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/delete")
                            .param("id", UUID.randomUUID().toString()))
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
                            .param("id", playlist.getId().toString()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Non-existing playlist")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/playlist/copy")
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
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/view").param("id", playlist.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/edit").param("id", playlist.getId().toString())).andExpect(status().isFound())
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
            mvc.perform(get("/playlist/view").param("id", playlist.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/edit").param("id", playlist.getId().toString())).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/delete").param("id", playlist.getId().toString())).andExpect(status().isForbidden());
            mvc.perform(post("/playlist/save")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/copy")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/view").param("id", playlist.getId().toString())).andExpect(status().isOk());
            mvc.perform(get("/playlist/create")).andExpect(status().isOk());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isNotFound());
        }

    }

}
