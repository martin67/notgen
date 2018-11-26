package se.terrassorkestern.notgen2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class InstrumentRestController {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @GetMapping("/api/instrument")
    public List<Instrument> index() {
        return instrumentRepository.findAll();
    }

    @GetMapping("/api/instrument/{id}")
    public Optional<Instrument> show(@PathVariable String id) {
        int instrumentId = Integer.parseInt(id);
        return instrumentRepository.findById(instrumentId);
    }

    @PostMapping("/api/instrument/search")
    public List<Instrument> search(@RequestBody Map<String, String> body) {
        String searchTerm = body.get("text");
        return instrumentRepository.findByName(searchTerm);
    }

    @PostMapping("/api/instrument")
    public Instrument create(@RequestBody Map<String, String> body) {
        int instrumentId = Integer.parseInt(body.get("id"));
        String name = body.get("name");
        String shortName = body.get("shortName");
        int sortOrder = Integer.parseInt(body.get("sortOrder"));
        boolean standard = Boolean.parseBoolean(body.get("standard"));
        return instrumentRepository.save(new Instrument(name, shortName, sortOrder, standard));
    }

    @PutMapping("/api/instrument/{id}")
    public Instrument update(@PathVariable String id, @RequestBody Map<String, String> body) {
        int instrumentId = Integer.parseInt(id);
        Instrument instrument = instrumentRepository.findById(instrumentId).get();
        instrument.setName(body.get("name"));
        instrument.setShortName(body.get("shortName"));
        instrument.setSortOrder(Integer.parseInt(body.get("sortOrder")));
        instrument.setStandard(Boolean.parseBoolean(body.get("standard")));
        return instrumentRepository.save(instrument);
    }

    @DeleteMapping("/api/instrument/{id}")
    public boolean delete(@PathVariable String id) {
        int instrumentId = Integer.parseInt(id);
        instrumentRepository.deleteById(instrumentId);
        return true;
    }
}
