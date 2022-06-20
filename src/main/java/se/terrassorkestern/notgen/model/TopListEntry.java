package se.terrassorkestern.notgen.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopListEntry {
    private String name;
    private long value;
}
