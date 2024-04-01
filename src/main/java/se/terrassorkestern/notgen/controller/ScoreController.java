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
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import se.terrassorkestern.notgen.model.*;
import se.terrassorkestern.notgen.repository.ConfigurationKeyRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.SongOcrService;
import se.terrassorkestern.notgen.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/score")
@SessionAttributes({"score", "instruments", "configurations"})
public class ScoreController extends CommonController {
    public static final String ATTRIBUTE_ONE_SCORE = "score";
    public static final String ATTRIBUTE_ALL_INSTRUMENTS = "instruments";
    public static final String ATTRIBUTE_ALL_CONFIGURATIONS = "configurations";
    public static final String ATTRIBUTE_ALL_SCORES = "scores";
    public static final String REDIRECT_SCORE_LIST = "redirect:/score/list";
    public static final String VIEW_SCORE_EDIT = "score/edit";
    public static final String VIEW_SCORE_VIEW = "score/view";
    public static final String VIEW_SCORE_LIST = "score/list";
    public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @Value("${se.terrassorkestern.notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${se.terrassorkestern.notgen.ocr.songids:0}")
    private String ocrSongIds;

    private final ActiveBand activeBand;
    private final ScoreRepository scoreRepository;
    private final ConverterService converterService;
    private final SongOcrService songOcrService;
    private final StorageService storageService;
    private final ConfigurationKeyRepository configurationKeyRepository;

    public ScoreController(ActiveBand activeBand, ScoreRepository scoreRepository,
                           ConverterService converterService,
                           SongOcrService songOcrService, StorageService storageService,
                           ConfigurationKeyRepository configurationKeyRepository) {
        this.activeBand = activeBand;
        this.scoreRepository = scoreRepository;
        this.converterService = converterService;
        this.songOcrService = songOcrService;
        this.storageService = storageService;
        this.configurationKeyRepository = configurationKeyRepository;
    }

    @GetMapping("/list")
    public String songList(Model model) {
        model.addAttribute(ATTRIBUTE_ALL_SCORES, getScores());
        return VIEW_SCORE_LIST;
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String delete(@PathVariable("id") UUID id, SessionStatus sessionStatus) {
        var score = getScore(id);
        log.info("Tar bort låt {} [{}]", score.getTitle(), score.getId());
        scoreRepository.delete(score);
        sessionStatus.setComplete();
        return REDIRECT_SCORE_LIST;
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable("id") UUID id, Model model) {
        var score = getScore(id);
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        model.addAttribute("settings", getSettings());
        return VIEW_SCORE_VIEW;
    }

    @GetMapping("/edit/arr/{id}")
    public String editConfig(@PathVariable("id") UUID arrangementId, @ModelAttribute Score score, Model model) {
        model.addAttribute("arrangement", score.getArrangement(arrangementId));
        return "score/edit :: configModalContents";
    }

    @GetMapping("/edit/link/{id}")
    public String editLink(@PathVariable("id") UUID linkId, @ModelAttribute Score score, Model model) {
        Link link;
        if (linkId.equals(NULL_UUID)) {
            link = new Link();
            link.setId(NULL_UUID);
        } else {
            link = score.getLink(linkId);
        }
        model.addAttribute("link", link);
        return "score/edit :: linkModalContents";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") UUID id, Model model) {
        var score = getScore(id);
        var arrangement = score.getDefaultArrangement();
        // Check if the score has a song instrument. Only one for now
        if (arrangement != null && enableOcr) {
            var songId = UUID.fromString(ocrSongIds);
            model.addAttribute("doSongOcr",
                    arrangement.getInstruments().stream().anyMatch(instrument -> instrument.getId().equals(songId)) ? "true" : "false");
        }
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        model.addAttribute(ATTRIBUTE_ALL_CONFIGURATIONS, configurationKeyRepository.findAll());
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/create")
    public String create(Model model) {
        var score = new Score();
        score.setBand(activeBand.getBand());
        var arrangement = new Arrangement();
        score.addArrangement(arrangement);
        score.setDefaultArrangement(arrangement);

        // Fyll på med standardinstrumenten så går det lite fortare att editera...
        List<Instrument> preloadedInstruments;
        if (activeBand.getBand().getStandardSetting() != null) {
            var standardSetting = getSetting(activeBand.getBand().getStandardSetting().getId());
            preloadedInstruments = new ArrayList<>(standardSetting.getInstruments());
            preloadedInstruments.sort(null);
        } else {
            preloadedInstruments = getInstruments();
        }
        for (var instrument : preloadedInstruments) {
            arrangement.addArrangementPart(new ArrangementPart(arrangement, instrument));
        }
        model.addAttribute(ATTRIBUTE_ONE_SCORE, score);
        model.addAttribute(ATTRIBUTE_ALL_INSTRUMENTS, getInstruments());
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"save"})
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String save(@Valid @ModelAttribute Score score,
                       @RequestParam(value = "defaultArrangementIndex", defaultValue = "0") int defaultArrangementIndex,
                       Errors errors,
                       SessionStatus sessionStatus) {
        if (errors.hasErrors()) {
            return VIEW_SCORE_EDIT;
        }
        log.info("Sparar låt {} [{}]", score.getTitle(), score.getId());
        if (!score.getArrangements().isEmpty()) {
            score.setDefaultArrangement(score.getArrangements().get(defaultArrangementIndex));
        }

        // Hantera sångstämma kontra inlagd sångtext
        var song = getInstruments().stream().filter(Instrument::isSong).findFirst();
        // Kolla först om bandet har en sångstämma överhuvudtaget
        if (song.isPresent()) {
            for (var arrangement : score.getArrangements()) {
                var songPart = arrangement.getArrangementPart(song.get());
                // Om det inte längre finns någon text så ta bort ev. fejkad sångentry
                if (score.getText().isEmpty() && songPart.isPresent() && songPart.get().getPage() == 0) {
                    arrangement.removeArrangementPart(songPart.get());
                    // Lägg till en fejkad sång om det finns text men ingen riktig sång
                    // Den fejkade texten har sida 0
                } else if (!score.getText().isEmpty() && songPart.isEmpty()) {
                    var arrangementPart = new ArrangementPart(arrangement, song.get());
                    arrangementPart.setPage(0);
                    arrangementPart.setLength(0);
                    arrangement.addArrangementPart(arrangementPart);
                }
            }
        }

        scoreRepository.save(score);
        sessionStatus.setComplete();
        return REDIRECT_SCORE_LIST;
    }

    @PostMapping(value = "/submit", params = {"addArrangement"})
    public String addArrangement(@ModelAttribute("score") Score score) {
        var arrangement = new Arrangement();
        score.addArrangement(arrangement);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteArrangement"})
    public String deleteArrangement(@ModelAttribute("score") Score score,
                                    @RequestParam("deleteArrangement") String arrangementId) {
        var arrangement = score.getArrangement(arrangementId);
        score.removeArrangement(arrangement);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"addArrangementPart"})
    public String addArrangementPart(@ModelAttribute("score") Score score,
                                     @RequestParam("addArrangementPart") String arrangementId) {
        var arrangement = score.getArrangement(arrangementId);
        arrangement.addArrangementPart(new ArrangementPart());
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteArrangementPart"})
    public String deleteArrangementPart(@ModelAttribute("score") Score score,
                                        @RequestParam("deleteArrangementPart") String arrangementPartAndIndex) {
        var arrangement = score.getArrangement(arrangementPartAndIndex.substring(0, 36));
        int rowIndex = Integer.parseInt(arrangementPartAndIndex.substring(37));
        // A bit of a kludge. Need to get record n of a sorted set.
        ArrangementPart arrangementPart = null;
        int counter = 0;
        for (var ap : arrangement.getArrangementParts()) {
            if (rowIndex == counter) {
                arrangementPart = ap;
                break;
            }
            counter++;
        }

        arrangement.removeArrangementPart(arrangementPart);
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"upload"})
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String upload(@ModelAttribute("score") Score score,
                         @RequestPart("file") MultipartFile file,
                         @RequestParam("file_type") NgFileType fileType,
                         @RequestParam(name = "file_name", required = false) String fileName) {
        log.info("upload: {}, score id: {}, type: {}, name: {}", file.getOriginalFilename(), score.getId(), fileType, fileName);
        var ngFile = storageService.uploadFile(file);
        ngFile.setType(fileType);
        if (fileName != null) {
            ngFile.setName(fileName);
        }
        score.addFile(ngFile);
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/downloadFile")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public ResponseEntity<InputStreamResource> downloadFile(@ModelAttribute("score") Score score,
                                                            @RequestParam("file_id") UUID fileId) {
        var file = score.getFile(fileId);
        log.info("download: {}, file id: {}", file.getOriginalFilename(), file.getId());
        var responseHeaders = new HttpHeaders();
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
        var file = score.getFile(fileId);
        log.info("view: {}, file id: {}", file.getOriginalFilename(), file.getId());
        return ResponseEntity.ok()
                .contentType(file.getContentType())
                .body(new InputStreamResource(storageService.downloadFile(file)));
    }

    @PostMapping(value = "/submit", params = {"editLink"})
    public String editLink(@ModelAttribute("score") Score score,
                           @RequestParam("link_id") UUID linkId,
                           @RequestParam("link_name") String linkName,
                           @RequestParam("link_uri") String linkUri,
                           @RequestParam("link_type") LinkType linkType,
                           @RequestParam(name = "link_comment", required = false) String linkComment) {
        if (linkId.equals(NULL_UUID)) {
            score.addLink(new Link(linkUri, linkType, linkName, linkComment));
        } else {
            var link = score.getLink(linkId);
            link.setName(linkName);
            link.setUri(URI.create(linkUri));
            link.setType(linkType);
            link.setComment(linkComment);
        }
        return VIEW_SCORE_EDIT;
    }

    @PostMapping(value = "/submit", params = {"deleteLink"})
    public String addLink(@ModelAttribute("score") Score score,
                          @RequestParam("deleteLink") UUID linkId) {
        score.removeLink(linkId);
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/deleteFile")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String deleteFile(@ModelAttribute("score") Score score,
                             @RequestParam("file_id") UUID fileId) {
        score.removeFile(fileId);
        return VIEW_SCORE_EDIT;
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('EDIT_SONG')")
    public String convert(@RequestParam("id") UUID id, SessionStatus sessionStatus) throws IOException {
        converterService.convert(List.of(getScore(id)));
        sessionStatus.setComplete();
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