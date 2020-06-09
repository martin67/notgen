package se.terrassorkestern.notgen2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.notelister.NoteListerController;
import se.terrassorkestern.notgen2.notelister.NoteListerService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(NoteListerController.class)
class NoteListerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NoteListerService noteListerService;

    // write test cases here

    @Test
    @WithMockUser
    void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/noteLister"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/noteLister/generate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "UPDATE_TOC")
    void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/noteLister"))
                .andExpect(status().isOk());

        mvc.perform(get("/noteLister/generate"))
                .andExpect(status().isOk());
    }

}
