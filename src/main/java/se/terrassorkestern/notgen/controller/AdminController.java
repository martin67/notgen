package se.terrassorkestern.notgen.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.imaging.ImageReadException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.service.AdminService;
import se.terrassorkestern.notgen.service.ImageDataExtractor;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
public class AdminController extends CommonController {

    public static final String REDIRECT_ADMIN = "redirect:/admin";
    private final ImageDataExtractor imageDataExtractor;
    private final ScoreRepository scoreRepository;
    private final AdminService adminService;
    private final JobLauncher jobLauncher;
    private final Job job;

    public AdminController(ImageDataExtractor imageDataExtractor,
                           ScoreRepository scoreRepository, AdminService adminService, JobLauncher jobLauncher, Job job) {
        this.imageDataExtractor = imageDataExtractor;
        this.scoreRepository = scoreRepository;
        this.adminService = adminService;
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    @GetMapping(value = {"", "/"})
    public String admin() {
        return "admin";
    }

    @GetMapping("/noteCreate")
    public String create() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        // Run with Spring batch as the job will take a long time
        jobLauncher.run(job, new JobParameters());
        return REDIRECT_ADMIN;
    }

    @GetMapping("/imageExtract")
    public String extract() throws IOException, ImageReadException {
        imageDataExtractor.extract(scoreRepository.findAll());
        return REDIRECT_ADMIN;
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException, SQLException {
        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        var formatter = DateTimeFormatter.ofPattern("yyMMdd_HHmm");
        var fileName = String.format("notgen_dump_%s.zip", LocalDateTime.now().format(formatter));
        response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        adminService.export(response.getOutputStream());
    }

}
