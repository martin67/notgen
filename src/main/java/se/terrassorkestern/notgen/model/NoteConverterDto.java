package se.terrassorkestern.notgen.model;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
public class NoteConverterDto {
    private List<Integer> selectedScores;
    private boolean allScores;
    private boolean upload;
    private boolean async;
}
