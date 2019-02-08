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
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.song.SongController;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(SongController.class)
public class SongControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SongRepository songRepository;

    @MockBean
    private InstrumentRepository instrumentRepository;


    // write test cases here

    @Test
    @WithMockUser(authorities = "EDIT_SONG")
    public void givenSongs_whenGetSongs_thenReturnJsonArray()
            throws Exception {

        Song song = new Song();
        song.setTitle("Hej vad det går bra!");


        List<Song> allSongs = Collections.singletonList(song);

        given(songRepository.findByOrderByTitle()).willReturn(allSongs);

        mvc.perform(get("/song/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("songList"))
                .andExpect(model().attributeExists("songs"))
                .andExpect(model().attribute("songs", hasSize(1)))
                .andExpect(model().attribute("songs", contains(samePropertyValuesAs(song))))
                .andExpect(content().string(
                        containsString(song.getTitle())));
    }

    @Test
    @WithMockUser
    public void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/song/new"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/song/save"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/song/delete"))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(authorities = "EDIT_SONG")
    public void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/song/new"))
                .andExpect(status().isOk());
    }

}
