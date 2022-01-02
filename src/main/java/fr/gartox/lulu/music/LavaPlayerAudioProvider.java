package fr.gartox.lulu.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;

import java.nio.ByteBuffer;

public class LavaPlayerAudioProvider extends AudioProvider {

    private final AudioPlayer player;
    private final MutableAudioFrame frame;

    public LavaPlayerAudioProvider(final AudioPlayer player) {
        // Allocate a ByteBuffer for Discord4J's AudioProvider to hold audio data for Discord
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        // Set LavaPlayer's AudioFrame to use the same buffer as Discord4J's
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(this.getBuffer());
        this.player = player;
    }

    @Override
    public boolean provide() {
        final boolean didProvide = this.player.provide(this.frame);

        if (didProvide) {
            this.getBuffer().flip();
        }

        return didProvide;
    }
}
