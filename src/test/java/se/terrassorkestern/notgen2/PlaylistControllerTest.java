package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;
import se.terrassorkestern.notgen2.playlist.*;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PlaylistController.class)
public class PlaylistControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PlaylistRepository playlistRepository;

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

        given(playlistRepository.findAll()).willReturn(allPlaylists);

        mvc.perform(get("/playlist/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
//                .andExpect(jsonPath("$", hasSize(1)))
//                .andExpect((ResultMatcher) jsonPath("$[0].name", is(playlist.getName())));
    }
}
