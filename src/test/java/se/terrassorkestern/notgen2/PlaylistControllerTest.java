package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.instrument.SettingRepository;
import se.terrassorkestern.notgen2.playlist.*;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(PlaylistController.class)
public class PlaylistControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PlaylistRepository playlistRepository;

    @MockBean
    private SettingRepository settingRepository;

    @MockBean
    private InstrumentRepository instrumentRepository;

    @MockBean
    private PlaylistPdfService playlistPdfService;

    @MockBean
    private PlaylistPackService playlistPackService;

    // write test cases here

    @Test
    public void givenPlaylists_whenGetPlaylists_thenReturnJsonArray()
            throws Exception {

        Playlist playlist = new Playlist();
        playlist.setName("Testlista");

        List<Playlist> allPlaylists = Collections.singletonList(playlist);

        given(playlistRepository.findAllByOrderByDateDesc()).willReturn(allPlaylists);

        mvc.perform(get("/playlist/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("playlistList"))
                .andExpect(model().attributeExists("playlists"))
                .andExpect(model().attribute("playlists", hasSize(1)))
                .andExpect(model().attribute("playlists", contains(samePropertyValuesAs(playlist))));
    }

    @Test
    @WithMockUser
    public void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/playlist/new"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/playlist/save"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/playlist/delete"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/playlist/copy"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "EDIT_PLAYLIST")
    public void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/playlist/new"))
                .andExpect(status().isOk());
    }

}
