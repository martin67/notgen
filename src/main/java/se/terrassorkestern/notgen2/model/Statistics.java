package se.terrassorkestern.notgen2.model;

import lombok.Data;

import java.util.List;

@Data
public class Statistics {
    private long numberOfSongs;
    private long numberOfScannedSongs;
    private long numberOfScannedPages;
    private long numberOfBytes;
    private long numberOfInstruments;
    private long numberOfPlaylists;

    private List<TopList> topGenres;
    private List<TopList> topComposers;
    private List<TopList> topArrangers;

    static class TopList {
        String name;
        int value;
    }
}
