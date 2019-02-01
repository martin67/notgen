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

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(InstrumentController.class)
public class InstrumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InstrumentRepository instrumentRepository;

    // write test cases here

    @Test
    @WithMockUser
    public void givenInstruments_whenGetInstruments_thenReturnJsonArray()
            throws Exception {

        Instrument sax = new Instrument();
        sax.setName("saxofon");
        sax.setSortOrder(10);

        List<Instrument> allInstruments = Arrays.asList(sax);

        given(instrumentRepository.findAll()).willReturn(allInstruments);

        mvc.perform(get("/instrument/list/")
                .contentType(MediaType.TEXT_HTML));
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(1)))
//                .andExpect((ResultMatcher) jsonPath("$[0].name", is(sax.getName())));
    }
}
