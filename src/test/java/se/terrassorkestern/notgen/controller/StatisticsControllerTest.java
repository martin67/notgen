package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.model.Statistics;
import se.terrassorkestern.notgen.repository.UserRepository;
import se.terrassorkestern.notgen.service.StatisticsService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
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