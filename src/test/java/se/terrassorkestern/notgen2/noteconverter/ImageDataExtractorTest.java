package se.terrassorkestern.notgen2.noteconverter;

import org.apache.commons.imaging.ImageReadException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import se.terrassorkestern.notgen2.score.Score;
import se.terrassorkestern.notgen2.score.ScoreRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class ImageDataExtractorTest {

    @Autowired
    ImageDataExtractor imageDataExtractor;

    @Autowired
    ScoreRepository scoreRepository;

    @Test
    @Transactional
    @WithMockUser
    void extract() throws IOException, ImageReadException {
        List<Score> scores = scoreRepository.findByTitle("Drömvalsen");
        imageDataExtractor.extract(scores);
    }

    @Test
    @Transactional
    @WithMockUser
    void extractMultipleScores() throws IOException, ImageReadException {
        List<Score> scores = scoreRepository.findByTitleContaining("valsen");
        imageDataExtractor.extract(scores);
    }

    @Test
    @Transactional
    @WithMockUser
    void problemsScores() throws IOException, ImageReadException {
        //List<Integer> scoreIds = List.of(227);
        List<Integer> scoreIds = Arrays.asList(277, 278);
        List<Score> scores = scoreRepository.findAllById(scoreIds);
        imageDataExtractor.extract(scores);
    }
}