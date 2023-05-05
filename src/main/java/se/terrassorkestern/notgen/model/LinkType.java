package se.terrassorkestern.notgen.model;

import lombok.Getter;

@Getter
public enum LinkType {
    SPOTIFY("SP", "Spotify"),
    YOUTUBE("YT", "YouTube");

    private final String code;
    private final String shortDescription;

    LinkType(String code, String shortDescription) {
        this.code = code;
        this.shortDescription = shortDescription;
    }
}
