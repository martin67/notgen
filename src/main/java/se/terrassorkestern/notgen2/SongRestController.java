package se.terrassorkestern.notgen2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class SongRestController {

    @Autowired
    private SongRepository songRepository;

    @GetMapping("/api/song")
    public List<Song> index(){
        return songRepository.findAll();
    }

    @GetMapping("/api/song/{id}")
    public Optional<Song> show(@PathVariable String id){
        int songId = Integer.parseInt(id);
        return songRepository.findById(songId);
    }

    @PostMapping("/api/song/search")
    public List<Song> search(@RequestBody Map<String, String> body){
        String searchTerm = body.get("text");
        return songRepository.findByTitle(searchTerm);
    }

    @DeleteMapping("/api/song/{id}")
    public boolean delete(@PathVariable String id){
        int songId = Integer.parseInt(id);
        songRepository.deleteById(songId);
        return true;
    }
}
