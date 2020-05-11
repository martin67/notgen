package se.terrassorkestern.notgen2.playlist;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen2.google.GoogleDriveService;
import se.terrassorkestern.notgen2.instrument.Instrument;
import se.terrassorkestern.notgen2.score.Score;
import se.terrassorkestern.notgen2.score.ScorePart;
import se.terrassorkestern.notgen2.score.ScoreRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@AllArgsConstructor
public class PlaylistPackService {

    private final @NonNull GoogleDriveService googleDriveService;
    private final @NonNull ScoreRepository scoreRepository;


    public String createPack(Playlist playlist, Instrument instrument, String filename) {

        String output;

        // Kolla först så att alla låtar i låtlistan går att mappa till riktiga låtar
        // Avbryt och varna om så är fallet
        // Bättre att kolla alla på en gång än att ta det allt eftersom
        log.debug("Kollar att alla låtar ur listan verkligen finns");
        for (PlaylistEntry playlistEntry : playlist.getPlaylistEntries()) {
            if (!playlistEntry.getBold() && scoreRepository.findByTitle(playlistEntry.getText()).isEmpty()) {
                log.warn("Hittar inte låten med titel: " + playlistEntry.getText());
                return null;
            }
        }

        // Create temp directory
        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory("notgen2-");
            log.debug("Creating temporary directory: " + tmpDir.toString());
        } catch (IOException e) {
            log.error("Can't create temporary directory");
            //e.printStackTrace();
            return null;
        }

        // Skapa PDF

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        output = tmpDir.toString() + File.separator + filename;
        pdfMergerUtility.setDestinationFileName(output);

        PDDocumentInformation pdd = new PDDocumentInformation();
        pdd.setAuthor("Terrassorkestern");
        pdd.setTitle(playlist.getName() + " " + playlist.getDate().toString() + " - " + instrument.getName());
        pdd.setSubject("Notpacke för " + instrument.getName());

        pdfMergerUtility.setDestinationDocumentInformation(pdd);


        // Loopa genom alla låtar och ladda ner det aktuella instruments PDF i en mapp.
        // Namnge dem med index så att det blir sorterat som i låtlistan

        int index = 0;
        for (PlaylistEntry playlistEntry : playlist.getPlaylistEntries()) {
            if (playlistEntry.getBold()) {
                continue;
            }

            Score score = scoreRepository.findByTitle(playlistEntry.getText()).get(0);
            if (score == null) {
                log.warn("Hittar inte låten med titel: " + playlistEntry.getText());
                continue;
            }

            ScorePart scorePart = score.getScoreParts().stream().
                    filter(p -> p.getInstrument().getId() == instrument.getId()).
                    findAny().
                    orElse(null);
            if (scorePart == null) {
                log.warn("Hittar ingen stämma för " + instrument.getName() + " i låten " + score.getTitle());
                continue;
            }

            if (scorePart.getGoogleId() == null) {
                log.warn("Inga scannade noter för " + instrument.getName() + " i låten " + score.getTitle());
                continue;
            }

            log.info("Downloading " + score.getTitle() + "/" + instrument.getName() + " [" + scorePart.getGoogleId() + "]");
            try {
                File f = googleDriveService.downloadFile(scorePart.getGoogleId(), index++, tmpDir);
                pdfMergerUtility.addSource(f);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            log.debug("Skapar en ny PDF");
            pdfMergerUtility.mergeDocuments(null);
            return output;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}