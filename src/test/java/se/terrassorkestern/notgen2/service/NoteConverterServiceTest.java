package se.terrassorkestern.notgen2.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import se.terrassorkestern.notgen2.model.Score;
import se.terrassorkestern.notgen2.repository.ScoreRepository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Transactional
class NoteConverterServiceTest {

    @Autowired
    NoteConverterService noteConverterService;
    @Autowired
    ScoreRepository scoreRepository;

    static final Integer[] exampleScores = {
            219,    // Jumpy Lullaby.zip, PNG, 2480x3508, 300x300, 1 bit greyscale (no conversion)
            268,    // Var det kärlek eller blott en lek.pdf, PNG, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            257,    // Swing it magistern.pdf, PNG, 2480x3507, -1x-1, 8 bit greyscale (no conversion)
            325,    // Bye Bye Baby.pdf, PNG, 2496x3508, -1x-1, 8 bit greyscale (no conversion)
            314,    // Öppna ditt fönster.pdf, PNG, 2872x2023, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            315,    // En äkta mexikanare.pdf, PNG, 2872x2023, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            407,    // Löjtnantshjärtan.pdf, PNG, 2464x3072, 2464x3456, 2448x3456, 2432x3456, -1x-1, 8 bit greyscale (no conversion)
            337,    // Hawaiis sång.zip, JPEG, 1275x1755, 150x150, 24 bit YCbCr
            385,    // En herre i frack.zip, JPEG, 1275x1750, 150x150, 24 bit YCbCr
            368,    // Lili Marleen.zip, JPEG, 1275x1800, 1275x1755, 150x150, 24 bit YCbCr
            313,    // Världen är full av violer.pdf, JPEG, 1792x1216, -1x-1, 24 bit YCbCr (only rotating)
            567,    // KAK-Valsen.zip, JPEG, 2409x3437, 300x300, 24 bit YCbCr
            628,    // Sportstugevalsen.zip, JPEG, 2480x3501, 300x300, 24 bit YCbCr
            377,    // Klart till drabbning.pdf, JPEG, 2480x3507, 300x300, 24 bit YCbCr (no conversion)
            369,    // Little Old Lady.zip, JPEG, 2547x3508, 2480x3501, 300x300, 24 bit YCbCr
            371,    // Let's swing.zip, JPEG, 2547x3508, 300x300, 24 bit YCbCr
            563,    // Hon är min stora, stora kärlek.zip, JPEG, 2547x3508, 2480x3501, 300x300, 24 bit YCbCr
            488,    // Drömvalsen.zip, JPEG, 2550x3501, 300x300, 24 bit YCbCr
            573     // Cherie-Mona.zip, JPEG, 2576x(2864-3744), 300x300, 24 bit YCbCr
    };


    @Test
    @WithMockUser
    void convertOneScore() throws IOException {
        List<Score> scores = scoreRepository.findByTitle("Drömvalsen");
        noteConverterService.convert(scores, false);
    }

    @Disabled
    @Test
    @WithMockUser
    void convertMultipleScores() throws IOException {
        List<Score> scores = scoreRepository.findByTitleContaining("valsen");
        noteConverterService.convert(scores, false);
    }

    @Disabled
    @Test
    @WithMockUser
    void convertExampleScores() throws IOException {
        List<Score> scores = scoreRepository.findAllById(Arrays.asList(exampleScores));
        noteConverterService.convert(scores, false);
    }
}