package fr.gartox.lulu.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import fr.gartox.lulu.LuluApplication;

import java.util.HashMap;
import java.util.Map;

public class GuildMusicManager {
    private static final Map<Snowflake, GuildMusicManager> MANAGERS = new HashMap<>();
    public static AudioPlayerManager PLAYER_MANAGER;

    static {
        GuildMusicManager.PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        GuildMusicManager.PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(GuildMusicManager.PLAYER_MANAGER);
    }

    public static GuildMusicManager of(Snowflake id) {
        return GuildMusicManager.MANAGERS.computeIfAbsent(id, snowflake -> new GuildMusicManager());
    }

    private final AudioPlayer player;
    private final AudioTrackScheduler scheduler;
    private final LavaPlayerAudioProvider provider;

    private GuildMusicManager() {
        this.player = GuildMusicManager.PLAYER_MANAGER.createPlayer();
        this.scheduler = new AudioTrackScheduler(this.player);
        this.provider = new LavaPlayerAudioProvider(this.player);

        this.player.addListener(this.scheduler);
    }

    public AudioPlayer getPlayer() {
        return this.player;
    }

    public AudioTrackScheduler getScheduler() {
        return this.scheduler;
    }

    public LavaPlayerAudioProvider getProvider() {
        return this.provider;
    }
}
