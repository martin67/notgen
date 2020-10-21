package se.terrassorkestern.notgen2.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.model.Statistics;
import se.terrassorkestern.notgen2.service.StatisticsService;
import se.terrassorkestern.notgen2.user.UserRepository;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
@DisplayName("Statistics controller")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private StatisticsService statisticsService;


    @Test
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