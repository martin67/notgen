package se.terrassorkestern.notgen2.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.instrument.SettingRepository;
import se.terrassorkestern.notgen2.playlist.*;
import se.terrassorkestern.notgen2.user.UserRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PlaylistController.class)
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
    private UserRepository userRepository;

    @MockBean
    private PlaylistPdfService playlistPdfService;

    @MockBean
    private PlaylistPackService playlistPackService;


    @BeforeEach
    void initTest() throws IOException {
        Playlist foo = new Playlist();
        Playlist bar = new Playlist();
        Instrument sax = new Instrument();

        List<Playlist> allPlaylists = Stream.of(foo, bar).collect(Collectors.toList());

        given(playlistRepository.findAllByOrderByDateDesc()).willReturn(allPlaylists);
        given(playlistRepository.findById(1)).willReturn(Optional.of(foo));
        given(instrumentRepository.findById(1)).willReturn(Optional.of(sax));
        given(playlistPdfService.create(foo)).willReturn(new ByteArrayInputStream(new byte[0]));

        String sinkName = System.getProperty("os.name").toLowerCase().contains("windows") ? "NUL" : "/dev/null";
        given(playlistPackService.createPack(foo, sax, "playlist.pdf")).willReturn(sinkName);
    }

    @Test
    @DisplayName("List")
    @WithAnonymousUser
    void whenListPlaylists_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("playlistList"))
                .andExpect(model().attributeExists("playlists"))
                .andExpect(model().attribute("playlists", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_PLAYLIST")
    void whenNewPlaylist_thenReturnOk() throws Exception {
        mvc.perform(get("/playlist/new")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("playlistEdit"))
                .andExpect(model().attributeExists("playlist"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
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
                    .andExpect(view().name("playlistEdit"))
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
        @DisplayName("Anonymous")
        @WithAnonymousUser
        void whenSaveAnonymous_thenReturnRedirect() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("name", "Playlist 1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/playlist/list*"));
        }

        @Test
        @DisplayName("Add row")
        @WithAnonymousUser
        void whenSaveAddRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("addRow", "true"))
                    .andExpect(view().name("playlistEdit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Delete row")
        @WithAnonymousUser
        void whenSaveDeleteRow_thenReturnOk() throws Exception {
            mvc.perform(post("/playlist/save").with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("deleteRow", "true"))
                    .andExpect(view().name("playlistEdit"))
                    .andExpect(model().attributeExists("playlist"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
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
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void whenAccessProtectedContentAsAnonymousUser_redirectToLogin() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/new")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/edit?id=1")).andExpect(status().isOk());
            mvc.perform(get("/playlist/delete")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/playlist/copy")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/playlist/save").with(csrf())).andExpect(status().isOk());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/new")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/edit?id=1")).andExpect(status().isOk());
            mvc.perform(get("/playlist/delete")).andExpect(status().isForbidden());
            mvc.perform(post("/playlist/save").with(csrf())).andExpect(status().isOk());
            mvc.perform(get("/playlist/copy")).andExpect(status().isForbidden());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_PLAYLIST")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/playlist/list")).andExpect(status().isOk());
            mvc.perform(get("/playlist/new")).andExpect(status().isOk());
            mvc.perform(get("/playlist/nonexistent")).andExpect(status().isNotFound());
        }

    }

}
