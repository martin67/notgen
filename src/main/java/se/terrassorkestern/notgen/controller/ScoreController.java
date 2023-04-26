package se.terrassorkestern.notgen.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;
import se.terrassorkestern.notgen.service.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/score")
@SessionAttributes({"score", "instruments"})
public class ScoreController extends CommonController {
    @Value("${notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${notgen.ocr.songids:0}")
    private String ocrSongIds;

    private final ActiveBand activeBand;
    private final ScoreRepository scoreRepository;
    private final ConverterService converterService;
    private final SongOcrService songOcrService;
    private final StorageService storageService;

    public ScoreController(ActiveBand activeBand, ScoreRepository scoreRepository,
                           ConverterService converterService,
                           SongOcrService songOcrService, StorageService storageService) {
        this.activeBand = activeBand;
        this.scoreRepository = scoreRepository;
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
    public String delete(@RequestParam("id") UUID id) {
        Score score = getScore(id);
        log.info("Tar bort låt {} [{}]", score.getTitle(), score.getId());
        scoreRepository.delete(score);
        return "redirect:/score/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") UUID id, Model model) {
        Score score = getScore(id);
        model.addAttribute("score", score);
        model.addAttribute("instruments", getInstruments());
        model.addAttribute("settings", getSettings());
        return "score/view";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") UUID id, Model model) {
        Score score = getScore(id);
        Arrangement arrangement = score.getDefaultArrangement();
        // Check if the score has a song instrument. Only one for now
        if (enableOcr) {
            UUID songId = UUID.fromString(ocrSongIds);
            if (arrangement.getInstruments().stream().anyMatch(instrument -> instrument.getId().equals(songId))) {
                model.addAttribute("doSongOcr", "true");
            } else {
                model.addAttribute("doSongOcr", "false");
            }
        }
        model.addAttribute("score", score);
        model.addAttribute("instruments", getInstruments());
        return "score/edit";
    }

    @GetMapping("/create")
    public String create(Model model) {
        Score score = new Score();
        Arrangement arrangement = new Arrangement();
        score.addArrangement(arrangement);
        score.setDefaultArrangement(arrangement);

        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        for (Instrument instrument : getInstruments()) {
            arrangement.addArrangementPart(new ArrangementPart(arrangement, instrument));
        }
        model.addAttribute("score", score);
        model.addAttribute("instruments", getInstruments());
        return "score/edit";
    }

    @PostMapping(value = "/submit", params = {"save"})
    public String save(@Valid @ModelAttribute Score score,
                       @RequestParam("defaultArrangementIndex") int defaultArrangementIndex,
                       Errors errors) {
        if (errors.hasErrors()) {
            return "score/edit";
        }
        log.info("Sparar låt {} [{}]", score.getTitle(), score.getId());
        if (!score.getArrangements().isEmpty()) {
            score.setDefaultArrangement(score.getArrangements().get(defaultArrangementIndex));
        }
        scoreRepository.save(score);
        return "redirect:/score/list";
    }

    @PostMapping(value = "/submit", params = {"addArrangement"})
    public String addArrangement(@ModelAttribute("score") Score score) {
        Arrangement arrangement = new Arrangement();
        score.addArrangement(arrangement);
        return "score/edit";
    }

    @PostMapping(value = "/submit", params = {"deleteArrangement"})
    public String deleteArrangement(@ModelAttribute("score") Score score,
                                    @RequestParam("deleteArrangement") String arrangementId) {
        Arrangement arrangement = score.getArrangement(arrangementId);
        score.getArrangements().remove(arrangement);
        return "score/edit";
    }

    @PostMapping(value = "/submit", params = {"addArrangementPart"})
    public String addArrangementPart(@ModelAttribute("score") Score score,
                                     @RequestParam("addArrangementPart") String arrangementId) {
        Arrangement arrangement = score.getArrangement(arrangementId);
        arrangement.addArrangementPart(new ArrangementPart());
        return "score/edit";
    }

    @PostMapping(value = "/submit", params = {"deleteArrangementPart"})
    public String deleteArrangementPart(@ModelAttribute("score") Score score,
                                        @RequestParam("deleteArrangementPart") String arrangementPart) {
        Arrangement arrangement = score.getArrangement(arrangementPart.substring(0, 36));
        int rowIndex = Integer.parseInt(arrangementPart.substring(37));
        arrangement.getArrangementParts().remove(rowIndex);
        return "score/edit";
    }

    @PostMapping(value = "/submit", params = {"upload"})
    public String upload(@ModelAttribute("score") Score score,
                         @RequestPart("file") MultipartFile file,
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
    public ResponseEntity<InputStreamResource> downloadFile(@ModelAttribute("score") Score score,
                                                            @RequestParam("file_id") UUID fileId) {
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

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_SUPERADMIN')")
    @GetMapping("/viewFile")
    public ResponseEntity<InputStreamResource> viewFile(@ModelAttribute("score") Score score,
                                                        @RequestParam("file_id") UUID fileId) {
        NgFile file = score.getFile(fileId);
        log.info("view: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return ResponseEntity.ok()
                .contentType(file.getContentType())
                .body(new InputStreamResource(storageService.downloadFile(file)));
    }

    @PreAuthorize("hasAuthority('EDIT_SONG')")
    @GetMapping("/deleteFile")
    public String deleteFile(@ModelAttribute("score") Score score,
                             @RequestParam("file_id") UUID fileId, Model model) {
        NgFile file = score.getFile(fileId);
        score.getFiles().remove(file);
        log.info("delete: {}, file id: {}", file.getOriginalFilename(), file.getId());
        model.addAttribute("allInstruments", getInstruments());
        return "score/edit";
    }

    @GetMapping("/convert")
    public String convert(@RequestParam("id") UUID id) throws IOException, InterruptedException {
        converterService.convert(List.of(getScore(id)));
        return "redirect:/score/list";
    }

    @GetMapping("/edit/ocr")
    public @ResponseBody
    String ocr(@RequestParam("id") UUID id) throws Exception {
        return songOcrService.process(getScore(id));
    }

    @GetMapping(value = "/edit/scores.json")
    public @ResponseBody
    List<String> getTitleSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllTitles() : scoreRepository.getAllTitlesByBand(activeBand.getBand());
    }

    @GetMapping(value = "/edit/genres.json")
    public @ResponseBody
    List<String> getGenreSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllGenres() : scoreRepository.getAllGenresByBand(activeBand.getBand());
    }

    @GetMapping(value = "/edit/composers.json")
    public @ResponseBody
    List<String> getComposerSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllComposers() : scoreRepository.getAllComposersByBand(activeBand.getBand());
    }

    @GetMapping(value = "/edit/authors.json")
    public @ResponseBody
    List<String> getAuthorSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllAuthors() : scoreRepository.getAllAuthorsByBand(activeBand.getBand());
    }

    @GetMapping(value = "/edit/arrangers.json")
    public @ResponseBody
    List<String> getArrangerSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllArrangers() : scoreRepository.getAllArrangersByBand(activeBand.getBand());
    }

    @GetMapping(value = "/edit/publishers.json")
    public @ResponseBody
    List<String> getPublisherSuggestions() {
        return isSuperAdmin() ? scoreRepository.getAllPublishers() : scoreRepository.getAllPublishersByBand(activeBand.getBand());
    }

}