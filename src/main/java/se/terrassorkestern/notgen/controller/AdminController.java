package se.terrassorkestern.notgen.controller;

import org.apache.commons.imaging.ImageReadException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.AdminService;
import se.terrassorkestern.notgen.service.ConverterService;
import se.terrassorkestern.notgen.service.ImageDataExtractor;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ConverterService converterService;
    private final ImageDataExtractor imageDataExtractor;
    private final ScoreRepository scoreRepository;
    private final AdminService adminService;

    public AdminController(ConverterService converterService, ImageDataExtractor imageDataExtractor,
                           ScoreRepository scoreRepository, AdminService adminService) {
        this.converterService = converterService;
        this.imageDataExtractor = imageDataExtractor;
        this.scoreRepository = scoreRepository;
        this.adminService = adminService;
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

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException, SQLException {
        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"notgen_dump.zip\"");
        adminService.export(response.getOutputStream());
    }

}
