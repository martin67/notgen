package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum NgFileType {
    ARRANGEMENT("ARR", "Arrangemang", "Ett inscannat arr i zip eller pdf-format"),
    FULL_SCORE("FULLSCORE", "Partitur", "Partitur med alla stämmor"),
    OTHER("OTHER", "Annat", "Annat dokument");

    private final String code;
    private final String shortDescription;
    private final String longDescription;

    NgFileType(String code, String shortDescription, String longDescription) {
        this.code = code;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }

}

