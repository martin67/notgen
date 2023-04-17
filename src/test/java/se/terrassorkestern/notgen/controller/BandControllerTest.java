package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Band;
import se.terrassorkestern.notgen.repository.BandRepository;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(BandController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Band controller")
class BandControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BandRepository bandRepository;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Band band;

    @BeforeEach
    void init() {
        band = new Band();
        given(bandRepository.findById(band.getId())).willReturn(Optional.of(band));
    }

    @Test
    @DisplayName("List")
    @WithMockUser(authorities = "EDIT_BAND")
    void list() throws Exception {
        mvc.perform(get("/band/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("band/list"))
                .andExpect(model().attributeExists("bands"));
    }

    @Test
    @DisplayName("Edit")
    @WithMockUser(authorities = "EDIT_BAND")
    void edit() throws Exception {
        mvc.perform(get("/band/edit").param("id", String.valueOf(band.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("band/edit"))
                .andExpect(model().attributeExists("band"));
    }

    @Test
    @DisplayName("Create")
    @WithMockUser(authorities = "EDIT_BAND")
    void create() throws Exception {
        mvc.perform(get("/band/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("band/edit"))
                .andExpect(model().attributeExists("band"));
    }

    @Test
    @DisplayName("Delete")
    @WithMockUser(authorities = "EDIT_BAND")
    void delete() throws Exception {
        mvc.perform(get("/band/delete").param("id", String.valueOf(band.getId())))
                .andExpect(redirectedUrlPattern("/band/list*"));
    }

    @Test
    @DisplayName("Save")
    @WithMockUser(authorities = "EDIT_BAND")
    void save() throws Exception {
        mvc.perform(post("/band/save")
                        .with(csrf()))
                .andExpect(redirectedUrlPattern("/band/list*"));
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void accessToProtected_anonymousUser() throws Exception {
            mvc.perform(get("/band")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void accessToProtected_normalUser() throws Exception {
            mvc.perform(get("/band")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_BAND")
        void whenAccessProtectedContentAsAdminUser_returnOk() throws Exception {
            mvc.perform(get("/band/list")).andExpect(status().isOk());
            mvc.perform(get("/band/new")).andExpect(status().isOk());
        }
    }
}
