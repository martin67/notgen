package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.noteconverter.NoteConverterController;
import se.terrassorkestern.notgen2.noteconverter.NoteConverterService;
import se.terrassorkestern.notgen2.song.SongRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(NoteConverterController.class)
public class NoteConverterControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NoteConverterService noteConverterService;

    @MockBean
    private SongRepository songRepository;

    // write test cases here

    @Test
    @WithMockUser
    public void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/noteConverter"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/noteConverter/convert"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CONVERT_SCORE")
    public void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/noteConverter"))
                .andExpect(status().isOk());

//        mvc.perform(post("/noteConverter/convert"))
//                .andExpect(status().isOk());
    }

}
