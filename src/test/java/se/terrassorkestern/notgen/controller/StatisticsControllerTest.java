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
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.StatisticsService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static se.terrassorkestern.notgen.controller.StatisticsController.TEXT_CSV;

@WebMvcTest
@Import(StatisticsController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Statistics controller")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    @BeforeEach
    void setUp() {
        Statistics statistics = new Statistics();
        given(statisticsService.getStatistics()).willReturn(statistics);
    }

    @Test
    @DisplayName("Statistics")
    void statistics() throws Exception {
        mvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("statistics"))
                .andExpect(model().attributeExists("numberOfSongs"));
    }

    @Test
    @DisplayName("Score list")
    void list() throws Exception {
        mvc.perform(get("/statistics/scorelist"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_CSV));
    }

    @Test
    @DisplayName("Full list")
    void fullList() throws Exception {
        mvc.perform(get("/statistics/fullscorelist"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_CSV));
    }

    @Test
    @DisplayName("Unscanned scores")
    void unscanned() throws Exception {
        mvc.perform(get("/statistics/unscanned"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void anonymous() throws Exception {
            mvc.perform(get("/statistics")).andExpect(status().isOk());
            mvc.perform(get("/statistics/scorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/fullscorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/unscanned")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser
        void normal() throws Exception {
            mvc.perform(get("/statistics")).andExpect(status().isOk());
            mvc.perform(get("/statistics/scorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/fullscorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/unscanned")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin user")
        @WithMockUser(authorities = "EDIT_BAND")
        void admin() throws Exception {
            mvc.perform(get("/statistics")).andExpect(status().isOk());
            mvc.perform(get("/statistics/scorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/fullscorelist")).andExpect(status().isOk());
            mvc.perform(get("/statistics/unscanned")).andExpect(status().isOk());
        }
    }

}