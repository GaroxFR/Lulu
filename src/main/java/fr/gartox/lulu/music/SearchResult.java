package fr.gartox.lulu.music;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class SearchResult {

    private String errorMessage;
    private AudioTrack track;
    private AudioPlaylist playlist;
    private Type type;

    public static SearchResult of(AudioTrack track) {
        return new SearchResult(track);
    }

    public static SearchResult of(AudioPlaylist playlist) {
        return new SearchResult(playlist);
    }

    public static SearchResult error(String errorMessage) {
        return new SearchResult(errorMessage);
    }

    private SearchResult(String errorMessage) {
        this.errorMessage = errorMessage;
        this.type = Type.ERROR;
    }

    private SearchResult(AudioTrack track) {
        this.track = track;
        this.type = Type.TRACK;
    }

    private SearchResult(AudioPlaylist playlist) {
        this.playlist = playlist;
        this.type = Type.PLAYLIST;
    }

    public Type getType() {
        return this.type;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public AudioTrack getTrack() {
        return this.track;
    }

    public AudioPlaylist getPlaylist() {
        return this.playlist;
    }

    public enum Type {
        TRACK, PLAYLIST, ERROR;
    }
}
