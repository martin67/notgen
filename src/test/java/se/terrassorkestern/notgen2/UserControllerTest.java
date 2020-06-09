package se.terrassorkestern.notgen2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.user.RoleRepository;
import se.terrassorkestern.notgen2.user.UserController;
import se.terrassorkestern.notgen2.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    // write test cases here


    @Test
    @WithMockUser
    void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/user/list"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/user/new"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/user/delete"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "EDIT_USER")
    void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/user/list"))
                .andExpect(status().isOk());

        mvc.perform(get("/user/new"))
                .andExpect(status().isOk());

//        mvc.perform(get("/user/delete?id=0"))
//                .andExpect(status().isOk());

//        mvc.perform(get("/user/edit&id=0"))
//                .andExpect(status().isOk());
// Måste finnas en post att testa med...

//        mvc.perform(post("/user/save"))
//                .andExpect(status().isOk());
    }
}
