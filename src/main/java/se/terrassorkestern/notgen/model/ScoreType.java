package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum ScoreType {
    NOT_SCANNED("NOT_SCANNED", "Ej inscannad", "Inte inscannad, ingen bildbehandling"),
    PDF("PDF", "PDF, ingen bildbehandling", ""),
    PDF_R("PDF_R", "PDF, rotera höger", ""),
    PDF_L("PDF_L", "PDF, rotera vänster", ""),
    BW("BW", "Svartvitt", "Inscannat i svartvitt"),
    COLOR("COLOR", "Färg", "Inscannat i färg"),
    SCANNED_TRYCK_ARR("SWPRINT", "Original tryckarr", "Inscannat i färg, över högre hörnet linjerar");

    private final String code;
    private final String shortDescription;
    private final String longDescription;

    ScoreType(String code, String shortDescription, String longDescription) {
        this.code = code;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }
}
