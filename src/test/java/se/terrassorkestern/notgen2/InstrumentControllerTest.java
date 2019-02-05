package se.terrassorkestern.notgen2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.instrument.InstrumentController;
import se.terrassorkestern.notgen2.instrument.InstrumentRepository;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(InstrumentController.class)
public class InstrumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InstrumentRepository instrumentRepository;

    // write test cases here

    @Test
    @WithMockUser(authorities = "INSTRUMENT_EDIT")
    public void givenInstruments_whenGetInstruments_thenReturnJsonArray()
            throws Exception {

        Instrument sax = new Instrument();
        sax.setName("saxofon");
        sax.setSortOrder(10);

        List<Instrument> allInstruments = Collections.singletonList(sax);

        given(instrumentRepository.findByOrderByStandardDescSortOrder()).willReturn(allInstruments);

        mvc.perform(get("/instrument/list")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("instrumentList"))
                .andExpect(content().string(
                        containsString(sax.getName())));
//                .andExpect(jsonPath("$", hasSize(1)))
//                .andExpect((ResultMatcher) jsonPath("$[0].name", is(sax.getName())));
    }

    @Test
    @WithMockUser
    public void accessToProtected_normalUser() throws Exception {
        mvc.perform(get("/instrument/list"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/instrument/new"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/instrument/edit"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/instrument/save"))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(authorities = "INSTRUMENT_EDIT")
    public void accessToProtected_adminUser() throws Exception {
        mvc.perform(get("/instrument/list"))
                .andExpect(status().isOk());

        mvc.perform(get("/instrument/new"))
                .andExpect(status().isOk());

//        mvc.perform(get("/instrument/edit&id=0"))
//                .andExpect(status().isOk());
// Måste finnas en post att testa med...

        mvc.perform(post("/instrument/save"))
                .andExpect(status().isOk());
    }
}
