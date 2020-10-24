package se.terrassorkestern.notgen2.service;

import org.apache.commons.imaging.ImageReadException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import se.terrassorkestern.notgen2.model.Score;
import se.terrassorkestern.notgen2.repository.ScoreRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Transactional
class ImageDataExtractorTest {

    @Autowired
    ImageDataExtractor imageDataExtractor;
    @Autowired
    ScoreRepository scoreRepository;


    @Test
    @WithMockUser
    void extract() throws IOException, ImageReadException {
        List<Score> scores = scoreRepository.findByTitle("Drömvalsen");
        imageDataExtractor.extract(scores);
    }

    @Disabled
    @Test
    @WithMockUser
    void extractMultipleScores() throws IOException, ImageReadException {
        List<Score> scores = scoreRepository.findByTitleContaining("valsen");
        imageDataExtractor.extract(scores);
    }

    @Disabled
    @Test
    @WithMockUser
    void problemsScores() throws IOException, ImageReadException {
        List<Integer> scoreIds = Arrays.asList(277, 278);
        List<Score> scores = scoreRepository.findAllById(scoreIds);
        imageDataExtractor.extract(scores);
    }

}