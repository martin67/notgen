package se.terrassorkestern.notgen.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TopListEntry {
    private String name;
    private long value;
}
