package se.terrassorkestern.notgen;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.ConverterService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
@Transactional
@ActiveProfiles("full-data")
@Tag("manual")
class ImageProcessingTest {

    private final List<String> exampleScores = List.of(
            "Jumpy Lullaby",                     // BW, zip, PNG, 2480x3508, 300x300, 1 bit greyscale (no conversion)
            "Var det kärlek eller blott en lek", // PDF, pdf, PNG, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            "Swing it magistern",                // ???, pdf, PNG, 2480x3507, -1x-1, 8 bit greyscale (no conversion)
            "Bye Bye Baby",                      // PDF, pdf, PNG, 2496x3508, -1x-1, 8 bit greyscale (no conversion)
            "Öppna ditt fönster",                // BW,  pdf, PNG, 2872x2023, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            "En äkta mexikanare",                // BW,  pdf, PNG, 2872x2023, 1656x2338, -1x-1, 8 bit greyscale (no conversion)
            "Löjtnantshjärtan",                  // PDF, pdf, PNG, 2464x3072, 2464x3456, 2448x3456, 2432x3456, -1x-1, 8 bit greyscale (no conversion)
            "Hawaiis sång",                      // TRYCK, JPEG, 1275x1755, 150x150, 24 bit YCbCr
            "En herre i frack",                  // TRYCK, JPEG, 1275x1750, 150x150, 24 bit YCbCr
            "Lili Marleen",                      // TRYCK, JPEG, 1275x1800, 1275x1755, 150x150, 24 bit YCbCr
            "Världen är full av violer",         // PDF_L, pdf, JPEG, 1792x1216, -1x-1, 24 bit YCbCr (only rotating)
            "KAK-Valsen",                        // ???, zip, JPEG, 2409x3437, 300x300, 24 bit YCbCr
            "Sportstugevalsen",                  // TRYCK, zip, JPEG, 2480x3501, 300x300, 24 bit YCbCr
            "Klart till drabbning",              // PDF, pdf, JPEG, 2480x3507, 300x300, 24 bit YCbCr (no conversion)
            "Little Old Lady",                   // TRYCK, zip, JPEG, 2547x3508, 2480x3501, 300x300, 24 bit YCbCr
            "Let's swing",                       // ???, zip, JPEG, 2547x3508, 300x300, 24 bit YCbCr
            "Hon är min stora, stora kärlek",    // TRYCK, zip, JPEG, 2547x3508, 2480x3501, 300x300, 24 bit YCbCr
            "Drömvalsen",                        // TRYCK, zip, JPEG, 2550x3501, 300x300, 24 bit YCbCr
            "Cherie-Mona"                        // COLOR, zip, JPEG, 2576x(2864-3744), 300x300, 24 bit YCbCr
    );

    private final List<String> colorScores = List.of(
            "As time goes by  [Theselius]",
            "Blonda Charlie",
            "Charles Boogie",
            "Cherie-Mona",
            "Come on, boys, så ska' vi swinga",
            "Du kom…",
            "En liten vrå för två",
            "Ett glatt humör",
            "Express",
            "Hadelittan tjavs-tjavs",
            "Have you ever been in Guantanamo?",
            "Humpty-Dumpty",
            "I Nischni-Nowgorod",
            "Jag gitter bara bugga jitterbug med dej",
            "Jag är en liten prick",
            "Jamming",
            "Jitterbug från Söder  [Högstedt]",
            "King Street Blues",
            "Kultisswing",
            "Kärlek på ranson",
            "Köp inte en zebra",
            "Matilda, Matilda",
            "Mörka skyar",
            "Oh, Mammy!",
            "Sgittibaddi-Sgattibiddi-bopa",
            "Som en blixt från en strålande himmel",
            "Sommarnattens kärleksmelodi",
            "Three flat jump",
            "Två hjärtan i swing",
            "We haven't got any cigarettes but we've got swing"
    );

    // Svarta med vitt längst ut
    private final List<String> bwScores = List.of(
            "En liten blå elefant",
            "För smicker är en kvinna svag",
            "Han kommer och bankar",
            "Hela livet blir en härlig sommardag",
            "Hemma i våran kåk",
            "Knas-Johan på Storsvängen",
            "Med en sång på mina läppar"
    );

    // Svarta med vitt längst ut
    private final List<String> pdfScores = List.of(
            "Du är den enda  [Ehrling]",
            "Evening Dreams",
            "Ha-la-li",
            "Ho-Dadia-Da",
            "I like bananas because they have no bones",
            "La vie en rose",
            "Swing high - swing low",
            "Säg det med ett leende",
            "Var det kärlek eller blott en lek",
            "Vi drar till skogs med kärlek och musik"
    );

    private final List<String> differentTypeScores = List.of(
            "Palais stroll",        // BW
            "En månskenspromenad",  // null
            "Jamming",              // COLOR
            "Cherie-Mona",           // COLOR
            "One hundred per cent", // PDF
            "Swing ändå",           // PDF_L
            "Easy swing",           // PDF_R
            "Stockholmshambo",       // SWPRINT
            "Arbetslösa amoriner"   // SWPRINT
    );

    @Autowired
    ConverterService converterService;
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    InstrumentRepository instrumentRepository;
    @Autowired
    SettingRepository settingRepository;

    @Test
    @WithMockUser
    void convertOneScore() throws IOException {
        List<Score> scores = scoreRepository.findByTitle("Express");
        converterService.convert(scores);
    }

    @Test
    @WithMockUser
    void convertMultipleScores() throws IOException {
        List<Score> scores = scoreRepository.findByTitleContaining("valsen");
        converterService.convert(scores);
    }

    @Test
    @WithMockUser
    void convertExampleScores() throws IOException {
        List<Score> scores = new ArrayList<>();
        for (String name : differentTypeScores) {
            scores.addAll(scoreRepository.findByTitle(name));
        }
        for (Score score : scores) {
            //log.info("Score: {}, type: {}", score, score.getDefaultArrangement().getScoreType());
        }
        converterService.convert(scores);
    }

    @Test
    @WithMockUser
    void assembleOneScore() throws IOException {
        List<Score> scores = scoreRepository.findByTitle("Drömvalsen");
        List<Instrument> instruments = instrumentRepository.findByNameContaining("saxofon");
        InputStream is = converterService.assemble(scores, instruments, false);
        Files.copy(is, Path.of("test.pdf"), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    @WithMockUser
    void assembleTOScores() throws IOException {
        List<Score> scores = scoreRepository.findByTitleContaining("ögon");
        List<Setting> setting = settingRepository.findByName("Terrassorkestern");
        InputStream is = converterService.assemble(scores, setting.get(0), true);
        Files.copy(is, Path.of("test2.pdf"), StandardCopyOption.REPLACE_EXISTING);
    }

}