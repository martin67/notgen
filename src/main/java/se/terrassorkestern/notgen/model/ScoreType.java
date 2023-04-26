package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum ScoreType {
    NotScanned("NOT_SCANNED", "Ej inscannad", "Inte inscannad, ingen bildbehandling"),
    PDF("PDF", "PDF, ingen bildbehandling", ""),
    PDF_R("PDF_R", "PDF, rotera höger", ""),
    PDF_L("PDF_L", "PDF, rotera vänster", ""),
    BW("BW", "Svartvitt", "Inscannat i svartvitt"),
    Color("COLOR", "Färg", "Inscannat i färg"),
    ScannedTryckArr("SWPRINT", "Original tryckarr", "Inscannat i färg, över högre hörnet linjerar");

    private final String code;
    private final String shortDescription;
    private final String longDescription;

    ScoreType(String code, String shortDescription, String longDescription) {
        this.code = code;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }
}
