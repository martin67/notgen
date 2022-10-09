package se.terrassorkestern.notgen.controller;

import org.apache.commons.imaging.ImageReadException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.ImageDataExtractor;

import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ConverterService converterService;
    private final ImageDataExtractor imageDataExtractor;
    private final ScoreRepository scoreRepository;

    public AdminController(ConverterService converterService, ImageDataExtractor imageDataExtractor,
                           ScoreRepository scoreRepository) {
        this.converterService = converterService;
        this.imageDataExtractor = imageDataExtractor;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping(value = {"", "/"})
    public String admin() {
        return "admin";
    }

    @GetMapping("/noteCreate")
    public void create() throws IOException, InterruptedException {
        converterService.convert(scoreRepository.findAll());
    }

    @GetMapping("/imageExtract")
    public void extract() throws IOException, ImageReadException {
        imageDataExtractor.extract(scoreRepository.findAll());
    }
}
