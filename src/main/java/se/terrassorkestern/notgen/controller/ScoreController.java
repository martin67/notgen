package se.terrassorkestern.notgen.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;
import se.terrassorkestern.notgen.service.StorageService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/score")
@SessionAttributes("score")
public class ScoreController extends CommonController {
    @Value("${notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${notgen.ocr.songids:0}")
    private String ocrSongIds;

    private final ActiveBand activeBand;
    private final ScoreRepository scoreRepository;
    private final InstrumentRepository instrumentRepository;
    private final ConverterService converterService;
    private final SongOcrService songOcrService;
    private final StorageService storageService;

    public ScoreController(ActiveBand activeBand, ScoreRepository scoreRepository, InstrumentRepository instrumentRepository,
                           ConverterService converterService,
                           SongOcrService songOcrService, StorageService storageService) {
        this.activeBand = activeBand;
        this.scoreRepository = scoreRepository;
        this.instrumentRepository = instrumentRepository;
        this.converterService = converterService;
        this.songOcrService = songOcrService;
        this.storageService = storageService;
    }

    @GetMapping("/list")
    public String songList(Model model) {
        model.addAttribute("scores", getScores());
        return "score/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Integer id) {
        Score score = getScore(id);
        log.info("Tar bort låt {} [{}]", score.getTitle(), score.getId());
        scoreRepository.delete(score);
        return "redirect:/score/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Integer id, Model model) {
        model.addAttribute("score", getScore(id));
        model.addAttribute("settings", getSettings());
        return "score/view";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") Integer id, Model model) {
        Score score = getScore(id);
        model.addAttribute("score", score);
        // Check if the score has a song instrument. Only one for now
        if (enableOcr) {
            int songId = Integer.parseInt(ocrSongIds);
            if (score.getInstruments().stream().anyMatch(instrument -> instrument.getId() == songId)) {
                model.addAttribute("doSongOcr", "true");
            } else {
                model.addAttribute("doSongOcr", "false");
            }
        }
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Score score = new Score();
        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        for (Instrument instrument : getInstruments()) {
            score.getScoreParts().add(new ScorePart(score, instrument));
        }
        model.addAttribute("score", score);
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @PostMapping(value = "/save", params = {"save"})
    public String save(@Valid @ModelAttribute Score score, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("allInstruments", getInstruments());
            return "score/edit";
        }
        log.info("Sparar låt {} [{}]", score.getTitle(), score.getId());
        for (Arrangement arrangement : score.getArrangements()) {
            for (ArrangementPart arrangementPart : arrangement.getArrangementParts()) {
                if (arrangementPart.getId() == null) {
                    arrangementPart.setId(new ArrangementPartId(arrangement.getId(), arrangementPart.getInstrument().getId()));
                    arrangementPart.setArrangement(arrangement);
                }
            }
        }
        scoreRepository.save(score);
        return "redirect:/score/list";
    }

    @PostMapping(value = "/save", params = {"addRow"})
    public String addRow(final Score score, Model model, @RequestParam("addRow") String arrName) {
        score.getArrangement(arrName).getArrangementParts().add(new ArrangementPart());
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @PostMapping(value = "/save", params = {"deleteRow"})
    public String deleteRow(final Score score, Model model, final HttpServletRequest req) {
        try {
            int scorePartId = Integer.parseInt(req.getParameter("deleteRow"));
            if (scorePartId < score.getScoreParts().size()) {
                score.getScoreParts().remove(scorePartId);
            } else {
                log.warn("Trying to remove non-existing score part {}", scorePartId);
            }
            //model.addAttribute("score", score);
            model.addAttribute("allInstruments", getInstruments());
        } catch (NumberFormatException ignore) {
        }
        return "score/edit";
    }

    @PostMapping(value = "/save", params = {"addArrangement"})
    public String addArrangement(final Score score, Model model) {
        score.addArrangement(new Arrangement("New arr"));
//        model.addAttribute("score", score);
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @PostMapping(value = "/save", params = {"upload"})
    public String upload(final Score score, @RequestPart("file") MultipartFile file,
                         @RequestParam("file_type") NgFileType fileType,
                         @RequestParam(name = "file_name", required = false) String fileName) {
        log.info("upload: {}, score id: {}, type: {}, name: {}", file.getOriginalFilename(), score.getId(), fileType, fileName);
        NgFile ngFile = storageService.uploadFile(file);
        ngFile.setType(fileType);
        if (fileName != null) {
            ngFile.setName(fileName);
        }
        score.getFiles().add(ngFile);
        return "score/edit";
    }

    @GetMapping("/downloadFile")
    public ResponseEntity<InputStreamResource> downloadFile(final Score score, @RequestParam("file_id") int fileId) {
        NgFile file = score.getFile(fileId);
        log.info("download: {}, file id: {}", file.getOriginalFilename(), file.getId());
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(file.getOriginalFilename())
                        .build()
        );
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(new InputStreamResource(storageService.downloadFile(file)));
    }

    @GetMapping("/viewFile")
    public ResponseEntity<InputStreamResource> viewFile(final Score score, @RequestParam("file_id") int fileId) {
        NgFile file = score.getFile(fileId);
        log.info("view: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return ResponseEntity.ok()
                .contentType(file.getContentType())
                .body(new InputStreamResource(storageService.downloadFile(file)));
    }

    @GetMapping("/deleteFile")
    public String deleteFile(final Score score, @RequestParam("file_id") Integer fileId) {
        NgFile file = score.getFile(fileId);
        //Score score = getScore(scoreId);
        score.getFiles().remove(file);
        log.info("delete: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return "score/edit";
    }

    @GetMapping("/convert")
    public String convert(@RequestParam("id") int id) throws IOException, InterruptedException {
        converterService.convert(List.of(getScore(id)));
        return "redirect:/score/list";
    }

    @GetMapping("/edit/ocr")
    public @ResponseBody
    String ocr(@RequestParam("id") int id) throws Exception {
        return songOcrService.process(getScore(id));
    }

    @GetMapping(value = "/scores.json")
    public @ResponseBody
    List<String> getTitleSuggestions() {
        return scoreRepository.getAllTitles();
    }

    @GetMapping(value = "/genres.json")
    public @ResponseBody
    List<String> getGenreSuggestions() {
        return scoreRepository.getAllGenres();
    }

    @GetMapping(value = "/composers.json")
    public @ResponseBody
    List<String> getComposerSuggestions() {
        return scoreRepository.getAllComposers();
    }

    @GetMapping(value = "/authors.json")
    public @ResponseBody
    List<String> getAuthorSuggestions() {
        return scoreRepository.getAllAuthors();
    }

    @GetMapping(value = "/arrangers.json")
    public @ResponseBody
    List<String> getArrangerSuggestions() {
        return scoreRepository.getAllArrangers();
    }

    @GetMapping(value = "/publishers.json")
    public @ResponseBody
    List<String> getPublisherSuggestions() {
        if (isSuperAdmin()) {
            return scoreRepository.getAllPublishers();
        } else {
            return scoreRepository.getAllPublishersByBand(activeBand.getBand());
        }
    }

}