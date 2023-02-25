package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.StatisticsService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    @Test
    @DisplayName("Statistics")
    void statistics() throws Exception {
        Statistics statistics = new Statistics();
        given(statisticsService.getStatistics()).willReturn(statistics);

        mvc.perform(get("/statistics")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("statistics"))
                .andExpect(model().attributeExists("numberOfSongs"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}