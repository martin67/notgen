package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum NgFileType {
    Arrangement("ARR"),
    Other("OTHER");

    private final String code;

    NgFileType(String code) {
        this.code = code;
    }
}

