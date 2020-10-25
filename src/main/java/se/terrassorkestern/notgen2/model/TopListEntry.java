package se.terrassorkestern.notgen2.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopListEntry {
    private String name;
    private long value;
}
