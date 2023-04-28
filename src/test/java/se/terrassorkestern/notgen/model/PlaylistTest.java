package se.terrassorkestern.notgen.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistTest {

    @Test
    void copy() {
        Playlist playlist1 = new Playlist();
        playlist1.setName("Test");
        playlist1.getPlaylistEntries().add(new PlaylistEntry());

        Playlist playlist2 = playlist1.copy();
        assertThat(playlist2.getName()).isEqualTo("Kopia av " + playlist1.getName());
        assertThat(playlist2.getPlaylistEntries()).hasSameSizeAs(playlist1.getPlaylistEntries());
    }
}