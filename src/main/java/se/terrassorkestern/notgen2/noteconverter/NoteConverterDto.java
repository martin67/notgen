package se.terrassorkestern.notgen2.noteconverter;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
class NoteConverterDto {
    private List<Integer> selectedSongs;
    private Boolean allSongs;
    private Boolean upload;
    private Boolean async;
}
