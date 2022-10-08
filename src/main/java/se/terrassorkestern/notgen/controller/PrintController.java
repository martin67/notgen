package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.NoteConverterService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/print")
public class PrintController {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final SettingRepository settingRepository;
    private final NoteConverterService noteConverterService;

    public PrintController(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository, SettingRepository settingRepository, NoteConverterService noteConverterService) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.settingRepository = settingRepository;
        this.noteConverterService = noteConverterService;
    }

    @GetMapping("/instrument")
    public String selectInstrument(@RequestParam(name = "id", required = false, defaultValue = "1") Integer id, Model model) {
        Instrument instrument = instrumentRepository.findById(id).get();
        model.addAttribute("scores", scoreRepository.findByScorePartsInstrumentOrderByTitle(instrument));
        model.addAttribute("instruments", instrumentRepository.findAll());
        model.addAttribute("selectedInstrument", id);
        return "printInstrument";
    }

    @GetMapping("/setting")
    public String printSetting(Model model) {
        model.addAttribute("scores", scoreRepository.findByOrderByTitle());
        model.addAttribute("settings", settingRepository.findAll());
        return "printSetting";
    }

    @GetMapping("/getpdf")
    public ResponseEntity<InputStreamResource> printInstrument(@RequestParam(name = "instrument_id") Integer instrumentId,
                                                               @RequestParam(name = "score_id") Integer scoreId) {

        List<Score> scores = scoreRepository.findById(scoreId).stream().toList();
        List<Instrument> instruments = List.of(instrumentRepository.findById(instrumentId).get());

        try (InputStream is = noteConverterService.assemble(scores, instruments, false)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=playlist.pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

}
