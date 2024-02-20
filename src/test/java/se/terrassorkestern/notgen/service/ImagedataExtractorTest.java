package se.terrassorkestern.notgen.service;

import org.apache.commons.imaging.ImageReadException;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.NgFile;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ImagedataRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes= ImageDataExtractor.class)
class ImagedataExtractorTest {

    @Autowired
    private ImageDataExtractor imageDataExtractor;
    @MockBean
    private ScoreRepository scoreRepository;
    @MockBean
    private StorageService storageService;
    @MockBean
    private ConverterService converterService;
    @MockBean
    private ImagedataRepository imagedataRepository;

    private Score score;


    @BeforeEach
    void setup() throws IOException, URISyntaxException {
        var path = Path.of(getClass().getClassLoader().getResource("testdata/1057.zip").toURI());

        score = new Score();
        var arrangement = new Arrangement();
        var ngFile = new NgFile();

        score.addArrangement(arrangement);
        score.setDefaultArrangement(arrangement);

        given(storageService.createTempDir()).willReturn(Path.of("."));
        given(storageService.downloadArrangement(null, null)).willReturn(path);
    }

    @Test
    void extract() throws IOException, ImageReadException {

        //var scores = scoreRepository.findByTitle("Drömvalsen");
        imageDataExtractor.extract(List.of(score));
    }

    @Disabled
    @Test
    @WithMockUser
    void extractMultipleScores() throws IOException, ImageReadException {
        var scores = scoreRepository.findByTitleContaining("valsen");
        imageDataExtractor.extract(scores);
    }

//    @Disabled
//    @Test
//    @WithMockUser
//    void problemsScores() throws IOException, ImageReadException {
//        List<Integer> scoreIds = Arrays.asList(277, 278);
//        List<Score> scores = scoreRepository.findAllById(scoreIds);
//        imageDataExtractor.extract(scores);
//    }

}