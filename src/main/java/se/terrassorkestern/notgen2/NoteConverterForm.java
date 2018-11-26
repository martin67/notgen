package se.terrassorkestern.notgen2;

import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class NoteConverterForm {
    private List<Integer> selectedSongs;
    private Boolean allSongs;
    private Boolean upload;


    NoteConverterForm() {}


    public List<Integer> getSelectedSongs() {
        return selectedSongs;
    }

    public void setSelectedSongs(List<Integer> selectedSongs) {
        this.selectedSongs = selectedSongs;
    }

    public Boolean getAllSongs() {
        return allSongs;
    }

    public void setAllSongs(Boolean allSongs) {
        this.allSongs = allSongs;
    }

    public Boolean getUpload() {
        return upload;
    }

    public void setUpload(Boolean upload) {
        this.upload = upload;
    }
}
