package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.*;
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
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SettingController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Setting controller")
class SettingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private ScoreRepository scoreRepository;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Setting setting;


    @BeforeEach
    void setUp() {
        Band band = new Band("The band", "The test band");
        setting = new Setting(band, "The setting");

        given(activeBand.getBand()).willReturn(band);
        given(settingRepository.findByBandAndId(band, setting.getId())).willReturn(Optional.of(setting));
        given(settingRepository.findByBand(band)).willReturn(List.of(setting));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("List")
    void settingList() throws Exception {
        mvc.perform(get("/setting/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("setting/list"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    @DisplayName("Edit")
    @WithMockUser(roles = "ADMIN")
    void settingEdit() throws Exception {
        mvc.perform(get("/setting/edit").param("id", setting.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("setting/edit"))
                .andExpect(model().attributeExists("setting"));
        mvc.perform(get("/setting/edit").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/setting/edit")).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Create")
    @WithMockUser(roles = "ADMIN")
    void settingNew() throws Exception {
        mvc.perform(get("/setting/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("setting/edit"))
                .andExpect(model().attributeExists("setting"));
    }

    @Test
    @DisplayName("Delete")
    @WithMockUser(roles = "ADMIN")
    void settingDelete() throws Exception {
        mvc.perform(get("/setting/delete").param("id", setting.getId().toString()))
                .andExpect(redirectedUrl("/setting/list"));
        mvc.perform(get("/setting/delete").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/setting/delete")).andExpect(status().is4xxClientError());

    }

    @Test
    @DisplayName("Save")
    @WithMockUser(roles = "ADMIN")
    void settingSave() throws Exception {
        mvc.perform(post("/setting/save").with(csrf()).sessionAttr("setting", setting))
                .andExpect(redirectedUrl("/setting/list"));
        setting.setName("");
        mvc.perform(post("/setting/save").with(csrf()).sessionAttr("setting", setting))
                .andExpect(status().isOk())
                .andExpect(view().name("setting/edit"));
        mvc.perform(post("/setting/save"))
                .andExpect(status().isForbidden());
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void anonymous() throws Exception {
            mvc.perform(get("/setting")).andExpect(status().isOk());
            mvc.perform(get("/setting/list")).andExpect(status().isOk());
            mvc.perform(get("/setting/edit").param("id", setting.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/setting/new")).andExpect(status().isOk());
            mvc.perform(get("/setting/delete").param("id", setting.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/setting/save").with(csrf()).sessionAttr("setting", setting))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/setting/xyzzy")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void normal() throws Exception {
            mvc.perform(get("/setting")).andExpect(status().isOk());
            mvc.perform(get("/setting/list")).andExpect(status().isOk());
            mvc.perform(get("/setting/edit").param("id", setting.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/setting/new")).andExpect(status().isOk());
            mvc.perform(get("/setting/delete").param("id", setting.getId().toString()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/setting/save").with(csrf()).sessionAttr("setting", setting))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/setting/xyzzy")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(roles = "ADMIN")
        void admin() throws Exception {
            mvc.perform(get("/setting")).andExpect(status().isOk());
            mvc.perform(get("/setting/list")).andExpect(status().isOk());
            mvc.perform(get("/setting/edit").param("id", setting.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/setting/new")).andExpect(status().isOk());
            mvc.perform(get("/setting/delete").param("id", setting.getId().toString()))
                    .andExpect(redirectedUrl("/setting/list"));
            mvc.perform(post("/setting/save").with(csrf()).sessionAttr("setting", setting))
                    .andExpect(redirectedUrl("/setting/list"));
            mvc.perform(get("/setting/xyzzy")).andExpect(status().isNotFound());
        }

    }

}