package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.terrassorkestern.notgen.model.ActiveBand;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Setting;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;
import se.terrassorkestern.notgen.service.ConverterService;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/print")
public class PrintController extends CommonController {

    private final ActiveBand activeBand;
    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final SettingRepository settingRepository;
    private final ConverterService converterService;

    public PrintController(ActiveBand activeBand, ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                           SettingRepository settingRepository,
                           ConverterService converterService) {
        this.activeBand = activeBand;
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.settingRepository = settingRepository;
        this.converterService = converterService;
    }

    @GetMapping("/instrument")
    public String selectInstrument(@RequestParam(name = "id", required = false) UUID id, Model model) {
        Instrument instrument;
        if (id == null) {
            instrument = instrumentRepository.findFirstByBand(activeBand.getBand()).orElseThrow();
            id = instrument.getId();
        } else {
            instrument = getInstrument(id);
        }

        var scores = scoreRepository.findByDefaultArrangement_ArrangementPartsInstrumentOrderByTitle(instrument);

        model.addAttribute("scores", scores);
        model.addAttribute("instruments", getInstruments());
        model.addAttribute("selectedInstrument", id);
        return "print/instrument";
    }

    @GetMapping("/setting")
    public String selectSetting(@RequestParam(name = "id", required = false) UUID id, Model model) {
        Setting setting;
        if (id == null) {
            setting = settingRepository.findFirstByBand(activeBand.getBand()).orElseThrow();
            id = setting.getId();
        } else {
            setting = getSetting(id);
        }

        var scores = scoreRepository.findByDefaultArrangement_ArrangementParts_InstrumentInOrderByTitleAsc(setting.getInstruments());

        model.addAttribute("scores", scores);
        model.addAttribute("settings", getSettings());
        model.addAttribute("selectedSetting", id);
        return "print/setting";
    }

    @GetMapping(value = "/arrangementPart", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PRINT_SCORE')")
    public ResponseEntity<InputStreamResource> printArrangementPart(@RequestParam(name = "instrument_id") UUID instrumentId,
                                                                    @RequestParam(name = "score_id") UUID scoreId) {
        var score = getScore(scoreId);
        var instrument = getInstrument(instrumentId);

        try (InputStream is = converterService.assemble(score, instrument)) {
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, createContentDisposition(score.getTitle(), instrument.getShortName()));
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/arrangement", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PRINT_SCORE')")
    public ResponseEntity<InputStreamResource> printArrangement(@RequestParam(name = "score_id") UUID scoreId,
                                                                @RequestParam(name = "arrangement_id", required = false) UUID arrangementId,
                                                                @RequestParam(name = "instrument_id") UUID instrumentId) {
        var score = getScore(scoreId);
        var instrument = getInstrument(instrumentId);
        Arrangement arrangement;
        if (arrangementId == null) {
            arrangement = score.getDefaultArrangement();
        } else {
            arrangement = score.getArrangement(arrangementId);
        }
        if (arrangement == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try (InputStream is = converterService.assemble(arrangement, instrument)) {
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, createContentDisposition(score.getTitle(), instrument.getShortName()));
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/score", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PRINT_SCORE')")
    public ResponseEntity<InputStreamResource> printScore(@RequestParam(name = "setting_id") UUID settingId,
                                                          @RequestParam(name = "score_id") UUID scoreId) {

        var score = getScore(scoreId);
        var setting = getSetting(settingId);

        try (InputStream is = converterService.assemble(score, setting)) {
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, createContentDisposition(score.getTitle(), setting.getName()));

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping(value = "/playlist", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PRINT_SCORE')")
    public ResponseEntity<InputStreamResource> printPlaylist(@RequestParam(name = "playlist_id") UUID playlistId,
                                                             @RequestParam(name = "instrument_id") UUID instrumentId) {
        var playlist = getPlaylist(playlistId);
        var instrument = getInstrument(instrumentId);

        try (InputStream is = converterService.assemble(playlist, instrument)) {
            var headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, createContentDisposition(playlist.getName(), instrument.getShortName()));

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(is));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private String createContentDisposition(String name, String shortName) {
        return "inline; filename=\"" + name + "\" (" + shortName + ").pdf";
    }

}
