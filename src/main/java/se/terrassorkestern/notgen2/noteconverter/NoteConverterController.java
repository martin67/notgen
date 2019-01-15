package se.terrassorkestern.notgen2.noteconverter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import se.terrassorkestern.notgen2.song.Song;
import se.terrassorkestern.notgen2.song.SongRepository;

import java.util.List;

@Slf4j
@Controller
public class NoteConverterController {

  @Autowired
  private SongRepository songRepository;
  @Autowired
  private NoteConverterService noteConverterService;


  public NoteConverterController() {
    log.debug("Constructor");
  }


  @GetMapping("/noteConverter")
  public String noteLister(Model model) {
    log.info("Nu är vi i noteConverter");

    List<Song> songs = songRepository.findByOrderByTitle();
    model.addAttribute("songs", songs);
    model.addAttribute("noteConverterForm", new NoteConverterForm());

    return "noteConverter";
  }

  @PostMapping("/noteConverter")
  public String handlePost(@ModelAttribute("noteConverterForm") NoteConverterForm noteConverterForm,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails.getAuthorities().contains(new SimpleGrantedAuthority("CONVERT_SCORE"))) {
      log.info("Nu är vi i noteConverter post");
      noteConverterService.convert(noteConverterForm.getSelectedSongs(),
          noteConverterForm.getAllSongs(), noteConverterForm.getUpload());
    }
    return "redirect:/noteConverter";
  }

}
