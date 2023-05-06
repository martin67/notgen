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
    public static final String ATTRIBUTE_ONE_SCORE = "score";
    public static final String ATTRIBUTE_ALL_INSTRUMENTS = "instruments";
    public static final String ATTRIBUTE_ALL_SCORES = "scores";
    public static final String REDIRECT_SCORE_LIST = "redirect:/score/list";
    public static final String VIEW_SCORE_EDIT = "score/edit";
    public static final String VIEW_SCORE_VIEW = "score/view";
    public static final String VIEW_SCORE_LIST = "score/list";
    @Value("${se.terrassorkestern.notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${se.terrassorkestern.notgen.ocr.songids:0}")
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
        model.addAttribute(ATTRIBUTE_ALL_SCORES, getScores());
        return VIEW_SCORE_LIST;
    }

    @GetMapping("/delete")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String delete(@RequestParam("id") UUID id) {
        Score score = getScore(id);
        log.info("Tar bort låt {} [{}]", score.getTitle(), score.getId());
        scoreRepository.delete(score);
        return REDIRECT_SCORE_LIST;
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") UUID id, Model model) {
        Score score = getScore(id);
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        model.addAttribute("settings", getSettings());
        return VIEW_SCORE_VIEW;
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") UUID id, Model model) {
        Score score = getScore(id);
        Arrangement arrangement = score.getDefaultArrangement();
        // Check if the score has a song instrument. Only one for now
        if (enableOcr) {
            UUID songId = UUID.fromString(ocrSongIds);
            model.addAttribute("doSongOcr",
                    arrangement.getInstruments().stream().anyMatch(instrument -> instrument.getId().equals(songId)) ? "true" : "false");
        }
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        return VIEW_SCORE_EDIT;
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
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"save"})
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String save(@Valid @ModelAttribute Score score,
                       @RequestParam("defaultArrangementIndex") int defaultArrangementIndex,
                       Errors errors) {
        if (errors.hasErrors()) {
            return VIEW_SCORE_EDIT;
        }
        log.info("Sparar låt {} [{}]", score.getTitle(), score.getId());
        if (!score.getArrangements().isEmpty()) {
            score.setDefaultArrangement(score.getArrangements().get(defaultArrangementIndex));
        }
        scoreRepository.save(score);
        return REDIRECT_SCORE_LIST;
    }

    @PostMapping(value = "/submit", params = {"addArrangement"})
    public String addArrangement(@ModelAttribute("score") Score score) {
        Arrangement arrangement = new Arrangement();
        score.addArrangement(arrangement);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteArrangement"})
    public String deleteArrangement(@ModelAttribute("score") Score score,
                                    @RequestParam("deleteArrangement") String arrangementId) {
        Arrangement arrangement = score.getArrangement(arrangementId);
        score.getArrangements().remove(arrangement);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"addArrangementPart"})
    public String addArrangementPart(@ModelAttribute("score") Score score,
                                     @RequestParam("addArrangementPart") String arrangementId) {
        Arrangement arrangement = score.getArrangement(arrangementId);
        arrangement.addArrangementPart(new ArrangementPart());
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteArrangementPart"})
    public String deleteArrangementPart(@ModelAttribute("score") Score score,
                                        @RequestParam("deleteArrangementPart") String arrangementPart) {
        Arrangement arrangement = score.getArrangement(arrangementPart.substring(0, 36));
        int rowIndex = Integer.parseInt(arrangementPart.substring(37));
        arrangement.getArrangementParts().remove(rowIndex);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"upload"})
    @PreAuthorize("hasAuthority('EDIT_SONG')")
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
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/downloadFile")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
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

    @GetMapping("/viewFile")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public ResponseEntity<InputStreamResource> viewFile(@ModelAttribute("score") Score score,
                                                        @RequestParam("file_id") UUID fileId) {
        NgFile file = score.getFile(fileId);
        log.info("view: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return ResponseEntity.ok()
                .contentType(file.getContentType())
                .body(new InputStreamResource(storageService.downloadFile(file)));
    }

    @PostMapping(value = "/submit", params = {"addLink"})
    public String addLink(@ModelAttribute("score") Score score,
                          @RequestParam("link_name") String linkName,
                          @RequestParam("link_uri") String linkUri,
                          @RequestParam("link_type") LinkType linkType,
                          @RequestParam(name = "link_comment", required = false) String linkComment) {
        score.getLinks().add(new Link(linkUri, linkType, linkName, linkComment));
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteLink"})
    public String addLink(@ModelAttribute("score") Score score,
                          @RequestParam("deleteLink") UUID linkId) {
        score.getLinks().remove(score.getLink(linkId));
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/deleteFile")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String deleteFile(@ModelAttribute("score") Score score,
                             @RequestParam("file_id") UUID fileId) {
        NgFile file = score.getFile(fileId);
        score.getFiles().remove(file);
        log.info("delete: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String convert(@RequestParam("id") UUID id) throws IOException, InterruptedException {
        converterService.convert(List.of(getScore(id)));
        return REDIRECT_SCORE_LIST;
    }

    @GetMapping("/edit/ocr")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
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