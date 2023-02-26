package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(PrintController.class)
@ContextConfiguration(classes = SecurityConfig.class)
@DisplayName("Print controller")
class PrintControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private InstrumentRepository instrumentRepository;
    @MockBean
    private SettingRepository settingRepository;
    @MockBean
    private PlaylistRepository playlistRepository;
    @MockBean
    private ConverterService converterService;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;


    @BeforeEach
    void initTest() {
        Score foo = new Score();
        Score bar = new Score();
        Instrument instrument = new Instrument();

        List<Score> allScores = List.of(foo, bar);
        given(scoreRepository.findByScorePartsInstrumentOrderByTitle(instrument)).willReturn(allScores);
        given(instrumentRepository.findById(2)).willReturn(Optional.of(instrument));
        given(instrumentRepository.findAll()).willReturn(List.of(instrument));
    }

    @Test
    @DisplayName("Print instrument")
    @WithMockUser(authorities = "PRINT_SCORE")
    void whenPrintInstrument_thenReturnOk() throws Exception {
        mvc.perform(get("/print/instrument").param("id", "2")
                        .contentType(MediaType.TEXT_HTML))
                .andExpect(view().name("printInstrument"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(2)))
                .andExpect(model().attributeExists("selectedInstrument"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}
