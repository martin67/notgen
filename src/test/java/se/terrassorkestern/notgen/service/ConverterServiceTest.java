package se.terrassorkestern.notgen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import se.terrassorkestern.notgen.CommonTestdata;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.ConfigurationKeyRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.converter.ImageProcessor;
import se.terrassorkestern.notgen.service.converter.PdfAssembler;
import se.terrassorkestern.notgen.service.converter.filters.AutoCropper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = {ConverterService.class, ImageProcessor.class, AutoCropper.class, PdfAssembler.class})
//@Transactional
//@Tag("manual")
//@Sql({"/full-data.sql"})
class ConverterServiceTest {

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
    @Autowired
    private ConverterService converterService;

    @MockBean
    private ScoreRepository scoreRepository;

    @MockBean
    private StorageService storageService;

    @MockBean
    private ConfigurationKeyRepository configurationKeyRepository;

    private List<Score> scores;

    @BeforeEach
    void setup() throws IOException, URISyntaxException {

        var commonTestdata = new CommonTestdata();
        //commonTestdata.setupRandom();
        commonTestdata.setupSingle();

        var band = commonTestdata.getBand();
        scores = commonTestdata.getScores(band);

        given(storageService.createTempDir(scores.get(0))).willReturn(Path.of("."));
        given(storageService.downloadArrangement(null, null))
                .willReturn(Path.of(getClass().getClassLoader().getResource("testdata/1057.zip").toURI()));
    }

    @Test
    void convertOneScore() throws IOException {
        //var scores = scoreRepository.findByTitle("Drömvalsen");
        converterService.convert(scores);
    }

    @Test
    @WithMockUser
    void convertMultipleScores() throws IOException {
        var scores = scoreRepository.findByTitleContaining("valsen");
        converterService.convert(scores);
    }

//    @Disabled
//    @Test
//    @WithMockUser
//    void convertExampleScores() throws IOException, InterruptedException {
//        List<Score> scores = scoreRepository.findAllById(Arrays.asList(exampleScores));
//        converterService.convert(scores);
//    }

//    @Test
//    void assembleOneScore() throws IOException {
//        var scores = scoreRepository.findByTitle("Drömvalsen");
//        var instruments = instrumentRepository.findByNameContaining("saxofon");
//        var inputStream = converterService.assemble(scores, instruments, false);
//        Files.copy(inputStream, Path.of("test.pdf"), StandardCopyOption.REPLACE_EXISTING);
//    }
//
//    @Test
//    @WithMockUser
//    void assembleTOScores() throws IOException {
//        var scores = scoreRepository.findByTitleContaining("ögon");
//        var setting = settingRepository.findByName("Terrassorkestern");
//        var inputStream = converterService.assemble(scores, setting.get(0), true);
//        Files.copy(inputStream, Path.of("test2.pdf"), StandardCopyOption.REPLACE_EXISTING);
//    }

}