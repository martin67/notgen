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
import se.terrassorkestern.notgen.model.Playlist;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.PlaylistRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.ConverterService;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Controller
@RequestMapping("/print")
public class PrintController {

    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final PlaylistRepository playlistRepository;
    private final SettingRepository settingRepository;
    private final ConverterService converterService;

    public PrintController(ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                           PlaylistRepository playlistRepository, SettingRepository settingRepository,
                           ConverterService converterService) {
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.playlistRepository = playlistRepository;
        this.settingRepository = settingRepository;
        this.converterService = converterService;
    }

    @GetMapping("/instrument")
    public String selectInstrument(@RequestParam(name = "id", required = false, defaultValue = "1") Integer id, Model model) {
        Instrument instrument = instrumentRepository.findById(id).orElseThrow();
        model.addAttribute("scores", scoreRepository.findByScorePartsInstrumentOrderByTitle(instrument));
        model.addAttribute("instruments", instrumentRepository.findAll());
        model.addAttribute("selectedInstrument", id);
        return "printInstrument";
    }

    @GetMapping("/setting")
    public String selectSetting(@RequestParam(name = "id", required = false, defaultValue = "1") Integer id, Model model) {
        Setting setting = settingRepository.findById(id).orElseThrow();
        model.addAttribute("scores", scoreRepository.findDistinctByScoreParts_InstrumentInOrderByTitleAsc(setting.getInstruments()));
        model.addAttribute("settings", settingRepository.findAll());
        model.addAttribute("selectedSetting", id);
        return "printSetting";
    }

    @GetMapping(value = "/getscorepart", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> printScorePart(@RequestParam(name = "instrument_id") Integer instrumentId,
                                                              @RequestParam(name = "score_id") Integer scoreId) {

        Score score = scoreRepository.findById(scoreId).orElseThrow();
        Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

        try (InputStream is = converterService.assemble(score, instrument)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + score.getTitle() + " (" + instrument.getShortName() + ").pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/getscore", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> printScore(@RequestParam(name = "setting_id") Integer settingId,
                                                          @RequestParam(name = "score_id") Integer scoreId) {

        Score score = scoreRepository.findById(scoreId).orElseThrow();
        Setting setting = settingRepository.findById(settingId).orElseThrow();

        try (InputStream is = converterService.assemble(score, setting)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + score.getTitle() + " (" + setting.getName() + ").pdf");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/playlist", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> printPlaylist(@RequestParam(name = "playlist_id") Integer playlistId,
                                                             @RequestParam(name = "instrument_id") Integer instrumentId) {

        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow();
        Instrument instrument = instrumentRepository.findById(instrumentId).orElseThrow();

        try (InputStream is = converterService.assemble(playlist, instrument)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=" + playlist.getName() + " (" + instrument.getShortName() + ").pdf");

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
