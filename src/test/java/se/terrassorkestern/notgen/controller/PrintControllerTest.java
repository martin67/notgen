package se.terrassorkestern.notgen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen.configuration.SecurityConfig;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.*;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.user.CustomOAuth2UserService;
import se.terrassorkestern.notgen.user.CustomOidcUserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
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
    private ActiveBand activeBand;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    private Instrument instrument;
    private Setting setting;
    private Score score;
    private Arrangement arrangement;
    private Playlist playlist;

    @BeforeEach
    void initTest() throws IOException {
        var band = new Band("The band", "The first test band");
        arrangement = new Arrangement("The arrangement");
        score = new Score(band, "The score");
        score.addArrangement(arrangement);
        score.setDefaultArrangement(arrangement);
        instrument = new Instrument(band, "Saxofon", "sax", 10);
        setting = new Setting(band, "My setting");
        playlist = new Playlist(band, "My playlist", "bla bla", LocalDate.now(), setting);

        var allScores = List.of(score);
        given(activeBand.getBand()).willReturn(band);
        given(scoreRepository.findByBandAndId(band, score.getId())).willReturn(Optional.of(score));
        given(scoreRepository.findByDefaultArrangement_ArrangementPartsInstrumentOrderByTitle(instrument)).willReturn(allScores);
        given(scoreRepository.findByDefaultArrangement_ArrangementParts_InstrumentInOrderByTitleAsc(setting.getInstruments())).willReturn(allScores);
        given(instrumentRepository.findByBandAndId(band, instrument.getId())).willReturn(Optional.of(instrument));
        given(instrumentRepository.findFirstByBand(band)).willReturn(Optional.of(instrument));
        given(instrumentRepository.findAll()).willReturn(List.of(instrument));
        given(settingRepository.findByBandAndId(band, setting.getId())).willReturn(Optional.of(setting));
        given(settingRepository.findFirstByBand(band)).willReturn(Optional.of(setting));
        given(playlistRepository.findByBandAndId(band, playlist.getId())).willReturn(Optional.of(playlist));
        given(converterService.assemble(score, instrument)).willReturn(new ByteArrayInputStream(new byte[0]));
        given(converterService.assemble(arrangement, instrument)).willReturn(new ByteArrayInputStream(new byte[0]));
        given(converterService.assemble(score, setting)).willReturn(new ByteArrayInputStream(new byte[0]));
        given(converterService.assemble(playlist, instrument)).willReturn(new ByteArrayInputStream(new byte[0]));
    }

    @Test
    @DisplayName("Select instrument")
    void selectInstrument() throws Exception {
        mvc.perform(get("/print/instrument").param("id", instrument.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("print/instrument"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(1)))
                .andExpect(model().attributeExists("selectedInstrument"));
        mvc.perform(get("/print/instrument"))
                .andExpect(status().isOk())
                .andExpect(view().name("print/instrument"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(1)))
                .andExpect(model().attributeExists("selectedInstrument"));
    }

    @Test
    @DisplayName("Select setting")
    void selectSetting() throws Exception {
        mvc.perform(get("/print/setting").param("id", setting.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("print/setting"))
                .andExpect(model().attributeExists("scores"))
                .andExpect(model().attribute("scores", hasSize(1)))
                .andExpect(model().attributeExists("selectedSetting"));
    }

    @Test
    @DisplayName("Print arrangement part")
    @WithMockUser(authorities = "PRINT_SCORE")
    void printArrangementPart() throws Exception {
        mvc.perform(get("/print/arrangementPart")
                        .param("instrument_id", instrument.getId().toString())
                        .param("score_id", score.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Print arrangement")
    @WithMockUser(authorities = "PRINT_SCORE")
    void printArrangement() throws Exception {
        mvc.perform(get("/print/arrangement")
                        .param("score_id", score.getId().toString())
                        .param("arrangement_id", arrangement.getId().toString())
                        .param("instrument_id", instrument.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
        mvc.perform(get("/print/arrangement")
                        .param("score_id", score.getId().toString())
                        .param("instrument_id", instrument.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Print score")
    @WithMockUser(authorities = "PRINT_SCORE")
    void printScore() throws Exception {
        mvc.perform(get("/print/score")
                        .param("setting_id", setting.getId().toString())
                        .param("score_id", score.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Print playlist")
    @WithMockUser(authorities = "PRINT_SCORE")
    void printPlaylist() throws Exception {
        mvc.perform(get("/print/playlist")
                        .param("playlist_id", playlist.getId().toString())
                        .param("instrument_id", instrument.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Nested
    @DisplayName("Access")
    class Access {

        @Test
        @DisplayName("Anonymous user")
        @WithAnonymousUser
        void anonymous() throws Exception {
            mvc.perform(get("/print/instrument").param("id", instrument.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/setting").param("id", setting.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/arrangementPart")
                            .param("instrument_id", instrument.getId().toString())
                            .param("score_id", score.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/print/arrangement")
                            .param("score_id", score.getId().toString())
                            .param("instrument_id", instrument.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/print/score")
                            .param("setting_id", setting.getId().toString())
                            .param("score_id", score.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/print/playlist")
                            .param("playlist_id", playlist.getId().toString())
                            .param("instrument_id", instrument.getId().toString()))
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("Normal user")
        @WithMockUser(authorities = "PRINT_SCORE")
        void normal() throws Exception {
            mvc.perform(get("/print/instrument").param("id", instrument.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/setting").param("id", setting.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/arrangementPart")
                            .param("instrument_id", instrument.getId().toString())
                            .param("score_id", score.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/arrangement")
                            .param("score_id", score.getId().toString())
                            .param("instrument_id", instrument.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/score")
                            .param("setting_id", setting.getId().toString())
                            .param("score_id", score.getId().toString()))
                    .andExpect(status().isOk());
            mvc.perform(get("/print/playlist")
                            .param("playlist_id", playlist.getId().toString())
                            .param("instrument_id", instrument.getId().toString()))
                    .andExpect(status().isOk());
        }
    }

}
