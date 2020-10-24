package se.terrassorkestern.notgen2.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.model.Instrument;
import se.terrassorkestern.notgen2.repository.InstrumentRepository;
import se.terrassorkestern.notgen2.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstrumentController.class)
@DisplayName("Instrument controller")
class InstrumentControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private UserRepository userRepository;


    @BeforeEach
    void initTest() {
        Instrument sax = new Instrument();
        sax.setName("saxofon");
        sax.setSortOrder(10);

        Instrument trumpet = new Instrument();
        trumpet.setName("trumpet");
        trumpet.setSortOrder(20);

        List<Instrument> allInstruments = Stream.of(sax, trumpet).collect(Collectors.toList());
        given(instrumentRepository.findAll()).willReturn(allInstruments);
        given(instrumentRepository.findById(1)).willReturn(Optional.of(sax));
    }

    @Test
    @DisplayName("List")
    @WithMockUser(authorities = "EDIT_INSTRUMENT")
    void whenListInstruments_thenReturnOk() throws Exception {
        mvc.perform(get("/instrument/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("instrumentList"))
                .andExpect(model().attributeExists("instruments"))
                .andExpect(model().attribute("instruments", hasSize(2)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("New")
    @WithMockUser(authorities = "EDIT_INSTRUMENT")
    void whenNewInstrument_thenReturnOk() throws Exception {
        mvc.perform(get("/instrument/new")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("instrumentEdit"))
                .andExpect(model().attributeExists("instrument"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Edit")
    class Edit {

        @Test
        @DisplayName("Existing instrument")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenEditValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/instrument/edit")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "1"))
                    .andExpect(view().name("instrumentEdit"))
                    .andExpect(model().attributeExists("instrument"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-existing instrument")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenEditNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/instrument/edit")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {

        @Test
        @DisplayName("Valid input")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenSaveValidInput_thenReturnRedirect() throws Exception {
            mvc.perform(post("/instrument/save").with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("name", "trombone")
                    .param("sortOrder", "5"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/instrument/list*"));
        }

        @Test
        @DisplayName("Invalid input")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenSaveInvalidInput_thenReturnReload() throws Exception {
            mvc.perform(post("/instrument/save").with(csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("id", "1"))
                    .andExpect(model().attributeExists("instrument"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("No CSRF")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenSaveWithoutCsrf_thenReturnForbidden() throws Exception {
            mvc.perform(post("/instrument/save")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Existing instrument")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenDeleteValidInput_thenReturnOk() throws Exception {
            mvc.perform(get("/instrument/delete")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "1"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("/instrument/list*"));
        }

        @Test
        @DisplayName("Non-existing instrument")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenDeleteNonValidInput_thenReturnNotFound() throws Exception {
            mvc.perform(get("/instrument/delete")
                    .contentType(MediaType.TEXT_HTML)
                    .param("id", "0"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void whenAccessProtectedContentAsAnonymousUser_redirectToLogin() throws Exception {
            mvc.perform(get("/instrument/list")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/instrument/new")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/instrument/edit")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/instrument/delete")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(post("/instrument/save").with(csrf())).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));

            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void whenAccessProtectedContentAsNormalUser_returnForbidden() throws Exception {
            mvc.perform(get("/instrument/list")).andExpect(status().isForbidden());
            mvc.perform(get("/instrument/new")).andExpect(status().isForbidden());
            mvc.perform(get("/instrument/edit")).andExpect(status().isForbidden());
            mvc.perform(get("/instrument/delete")).andExpect(status().isForbidden());
            mvc.perform(post("/instrument/save")).andExpect(status().isForbidden());

            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_INSTRUMENT")
        void whenAccessProtectedContentAsAdminUser_returnForbiddenOk() throws Exception {
            mvc.perform(get("/instrument/list")).andExpect(status().isOk());
            mvc.perform(get("/instrument/new")).andExpect(status().isOk());

            mvc.perform(get("/instrument/nonexistent")).andExpect(status().isNotFound());
        }

    }
}
