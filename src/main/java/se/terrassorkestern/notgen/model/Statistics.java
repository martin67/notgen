package se.terrassorkestern.notgen.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Statistics {
    private long numberOfSongs;
    private long numberOfArrangements;
    private long numberOfScannedSongs;
    private long numberOfScannedPages;
    private long numberOfInstruments;
    private long numberOfPlaylists;

    private List<TopListEntry> topGenres;
    private List<TopListEntry> topComposers;
    private List<TopListEntry> topArrangers;
    private List<TopListEntry> topAuthors;
    private List<TopListEntry> topPublishers;
}
