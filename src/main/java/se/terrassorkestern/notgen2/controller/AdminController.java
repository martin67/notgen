package se.terrassorkestern.notgen2.controller;

import org.apache.commons.imaging.ImageReadException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen2.repository.ScoreRepository;
import se.terrassorkestern.notgen2.service.ImageDataExtractor;

import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    final ImageDataExtractor imageDataExtractor;
    final ScoreRepository scoreRepository;


    public AdminController(ImageDataExtractor imageDataExtractor, ScoreRepository scoreRepository) {
        this.imageDataExtractor = imageDataExtractor;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping(value = {"", "/"})
    public String admin() {
        return "admin";
    }

    @GetMapping("/imageExtract")
    public void extract() throws IOException, ImageReadException {
        imageDataExtractor.extract(scoreRepository.findAll());
    }
}
