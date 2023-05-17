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
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(InstrumentController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Instrument controller")
class InstrumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InstrumentRepository instrumentRepository;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Instrument sax;


    @BeforeEach
    void initTest() {
        Band band1 = new Band("Band 1", "Band nr 1");
        Band band2 = new Band("Band 2", "Band nr 2");

        sax = new Instrument(band1, "Saxofon", "sax", 10);
        Instrument trumpet = new Instrument(band1, "Trumpet", "tp", 20);
        Instrument flute = new Instrument(band2, "Flöjt", "fl", 10);

        given(activeBand.getBand()).willReturn(band1);
        given(instrumentRepository.findByBandOrderBySortOrder(band1)).willReturn(List.of(sax, trumpet));
        doReturn(Optional.of(sax)).when(instrumentRepository).findByBandAndId(band1, sax.getId());
        doReturn(Optional.of(flute)).when(instrumentRepository).findByBandAndId(band1, flute.getId());
    }

    @Test
    @DisplayName("List")
    void instrumentList() throws Exception {
        mvc.perform(get("/instrument/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument/list"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attribute("instruments", hasSize(2)));
    }

    @Test
    @DisplayName("View")
    void instrumentView() throws Exception {
        mvc.perform(get("/instrument/view").param("id", sax.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument/view"));
    }

    @Test
    @DisplayName("Create")
    void instrumentCreate() throws Exception {
        mvc.perform(get("/instrument/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument/edit"))
                .andExpect(model().attributeExists("instrument"));
    }

    @Test
    @DisplayName("Edit")
    void instrumentEdit() throws Exception {
        mvc.perform(get("/instrument/edit").param("id", sax.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument/edit"))
                .andExpect(model().attributeExists("instrument"));
        mvc.perform(get("/instrument/edit").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/instrument/edit")).andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Save")
    @WithMockUser(authorities = "EDIT_INSTRUMENT")
    void instrumentSave() throws Exception {
        mvc.perform(post("/instrument/save").with(csrf()).sessionAttr("instrument", sax))
                .andExpect(redirectedUrl("/instrument/list"));
        sax.setName("");
        mvc.perform(post("/instrument/save").with(csrf()).sessionAttr("instrument", sax))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument/edit"));
        mvc.perform(post("/instrument/save"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete")
    @WithMockUser(authorities = "EDIT_INSTRUMENT")
    void instrumentDelete() throws Exception {
        mvc.perform(get("/instrument/delete").param("id", sax.getId().toString()))
                .andExpect(redirectedUrl("/instrument/list"));
        mvc.perform(get("/instrument/delete").param("id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void anonymous() throws Exception {
            mvc.perform(get("/instrument")).andExpect(status().isOk());
            mvc.perform(get("/instrument/list")).andExpect(status().isOk());
            mvc.perform(get("/instrument/view").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/create")).andExpect(status().isOk());
            mvc.perform(get("/instrument/edit").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/delete").param("id", sax.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/instrument/save").with(csrf()).sessionAttr("instrument", sax))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void normal() throws Exception {
            mvc.perform(get("/instrument/list")).andExpect(status().isOk());
            mvc.perform(get("/instrument/create")).andExpect(status().isOk());
            mvc.perform(get("/instrument/view").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/edit").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/delete").param("id", sax.getId().toString()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/instrument/save").with(csrf()).sessionAttr("instrument", sax))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void admin() throws Exception {
            mvc.perform(get("/instrument/list")).andExpect(status().isOk());
            mvc.perform(get("/instrument/create")).andExpect(status().isOk());
            mvc.perform(get("/instrument/view").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/edit").param("id", sax.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/instrument/delete").param("id", sax.getId().toString()))
                    .andExpect(redirectedUrl("/instrument/list"));
            mvc.perform(post("/instrument/save").with(csrf()).sessionAttr("instrument", sax))
                    .andExpect(redirectedUrl("/instrument/list"));
            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isNotFound());
        }

    }
}
