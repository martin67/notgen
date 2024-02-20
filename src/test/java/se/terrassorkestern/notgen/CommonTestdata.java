package se.terrassorkestern.notgen;

import se.terrassorkestern.notgen.model.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommonTestdata {
    private List<Band> bands;
    private List<Instrument> instruments;
    private List<Score> scores;

    public CommonTestdata() {
        bands = new ArrayList<>();
        instruments = new ArrayList<>();
        scores = new ArrayList<>();
    }

    public void setupRandom() {
        setupRandom( 1,  10, 3,  2, 5);
    }

    public void setupRandom(int numberOfBands, int numberOfInstruments, int numberOfScores, int numberOfArrangements, int numberOfArrangementParts) {
        if (numberOfArrangementParts > numberOfInstruments) {
            throw new IllegalArgumentException("Can't have more arrangement parts than instruments");
        }

        bands = new ArrayList<>();
        for (int b = 0; b < numberOfBands; b++) {
            var band = new Band("Band nr " + b, "Band number " + b);
            bands.add(band);
            for (int i = 0; i < numberOfInstruments; i++) {
                instruments.add(new Instrument(band, "Instrument " + i, "Instrument " + i + " for band " + band.getName(), i));
            }
            for (int s = 0; s < numberOfScores; s++) {
                var score = new Score(band, "Score " + s);
                scores.add(score);
                for (int a = 0; a < numberOfArrangements; a++) {
                    var arrangement = new Arrangement("Arrangement " + a);
                    score.addArrangement(arrangement);
                    if (a == 0) {
                        score.setDefaultArrangement(arrangement);
                    }
                    int page = 1;
                    for (int ap = 0; ap < numberOfArrangementParts; ap++) {
                        var arrangementPart = new ArrangementPart(arrangement, instruments.get(ap));
                        int length = 1 + (page % 2);
                        arrangementPart.setPage(page);
                        arrangementPart.setLength(length);
                        page += length;
                        arrangement.addArrangementPart(arrangementPart);
                    }

                    var ngFile = new NgFile("file.zip", NgFileType.ARRANGEMENT, "namn", "namn", "comment");
                    score.addFile(ngFile);
                    arrangement.setFile(ngFile);
                }
            }
        }
    }

    public void setupSingle() {
        var band = new Band("The band", "The testing band");
        bands = List.of(band);

        Instrument asx1 = new Instrument(band, "Alto sax 1", "asx1", 10);
        Instrument asx2 = new Instrument(band, "Alto sax 2", "asx2", 12);
        Instrument tsx = new Instrument(band, "Tenor sax", "tsx", 14);
        Instrument tp1 = new Instrument(band, "Trumpet 1", "tp1", 20);
        Instrument tp2 = new Instrument(band, "Trumpet 2", "tp2", 22);
        Instrument tb = new Instrument(band, "Trombone", "tb", 24);
        Instrument pno = new Instrument(band, "Piano", "pno", 30);
        Instrument bass = new Instrument(band, "Bass", "bass", 32);
        Instrument guit = new Instrument(band, "Guitar", "guit", 34);
        Instrument drums = new Instrument(band, "Drums", "drm", 36);
        Instrument vocal = new Instrument(band, "Vocals", "voc", 40);
        Instrument vio = new Instrument(band, "Violin", "vio", 50);
        instruments = List.of(asx1, asx2, tsx, tp1, tp2, tb, pno, bass, guit, drums, vocal, vio);

        Score score = new Score(band, "Hawaiis sång");
        scores = List.of(score);

        // testdata 1057 - Hawaiis sång
        try {
            var path = Path.of(getClass().getClassLoader().getResource("testdata/1057.zip").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        var arrangement = new Arrangement();
        var ngFile = new NgFile();
        score.addFile(ngFile);
        arrangement.setFile(ngFile);
        arrangement.addArrangementPart(new ArrangementPart(arrangement, vocal));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, asx1));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, tsx));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, asx2));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, tp1));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, bass));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, guit));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, vio));
        arrangement.addArrangementPart(new ArrangementPart(arrangement, pno));
        score.addArrangement(arrangement);
        score.setDefaultArrangement(arrangement);
    }

    public Band getBand() {
        return bands.get(0);
    }

    public List<Instrument> getInstruments(Band band) {
        return instruments.stream()
                .filter(i -> i.getBand() == band)
                .toList();
    }

    public List<Score> getScores(Band band) {
        return scores.stream()
                .filter(s -> s.getBand() == band)
                .toList();
    }

//    private Path createFile(String filename) throws URISyntaxException {
//        var path = Path.of(getClass().getClassLoader().getResource("testdata/" + filename).toURI());
//    }
}
