package se.terrassorkestern.notgen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import se.terrassorkestern.notgen.exceptions.NotFoundException;
import se.terrassorkestern.notgen.model.Organization;
import se.terrassorkestern.notgen.repository.OrganizationRepository;

import javax.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/organization")
public class OrganizationController {
    private final OrganizationRepository organizationRepository;


    public OrganizationController(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("organizations", organizationRepository.findAll());
        return "organizationList";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam("id") Integer id, Model model) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Organization %d not found", id)));
        model.addAttribute("organization", organization);
        return "organizationEdit";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("organization", new Organization());
        return "organizationEdit";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("id") Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Organization %d not found", id)));
        log.info("Tar bort organization {} [{}]", organization.getName(), organization.getId());
        organizationRepository.delete(organization);
        return "redirect:/organizationlist";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute Organization organization, Errors errors) {
        if (errors.hasErrors()) {
            return "organizationEdit";
        }
        log.info("Sparar organization {} [{}]", organization.getName(), organization.getId());
        organizationRepository.save(organization);
        return "redirect:/organization/list";
    }

}
