package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.notelister.NoteListerController;
import se.terrassorkestern.notgen2.notelister.NoteListerService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(NoteListerController.class)
public class NoteListerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NoteListerService noteListerService;

    // write test cases here

    @Test
    @WithMockUser
    public void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/noteLister"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/noteLister/generate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "UPDATE_TOC")
    public void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/noteLister"))
                .andExpect(status().isOk());

        mvc.perform(get("/noteLister/generate"))
                .andExpect(status().isOk());
    }

}
