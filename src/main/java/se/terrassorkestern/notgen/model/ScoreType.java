package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum ScoreType {
    NotScanned("NOT_SCANNED", "Ej inscannad", "Inte inscannad, ingen bildbehandling"),
    PDF("PDF", "PDF, ingen bildbehandling", ""),
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
