package fr.gartox.lulu.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class AudioTrackScheduler extends AudioEventAdapter {

    private final List<AudioTrack> queue;
    private final AudioPlayer player;

    public AudioTrackScheduler(AudioPlayer player) {
        this.queue = Collections.synchronizedList(new LinkedList<>());
        this.player = player;
    }

    public boolean play(AudioTrack track) {
        return this.play(track, false);
    }

    public boolean play(AudioTrack track, boolean force) {
        final boolean playing = this.player.startTrack(track, !force);

        if (!playing && track != null) {
            this.queue.add(track);
        }

        return playing;
    }

    public void skip() {
        if (this.queue.isEmpty()) {
            this.play(null, true);
        } else {
            this.play(this.queue.remove(0), true);
        }
    }

    public void pause() {
        this.player.setPaused(true);
    }

    public void resume() {
        this.player.setPaused(false);
    }

    public void stop() {
        this.queue.clear();
        this.player.stopTrack();
    }

    public AudioPlayer getPlayer() {
        return this.player;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            this.skip();
        }
    }
}
