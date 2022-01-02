package fr.gartox.lulu.listener;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.VoiceConnection;
import fr.gartox.lulu.music.GuildMusicManager;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class VoiceStateListener {

    public Mono<Void> handle(VoiceStateUpdateEvent event) {
        if (event.getOld().isEmpty() || event.getOld().get().getChannelId().isEmpty()) {
            return Mono.empty();
        }

        VoiceState old = event.getOld().get();

         return event.getOld().get().getGuild().flatMap(Guild::getVoiceConnection)
                .flatMap(VoiceConnection::getChannelId)
                .filter(channelId -> channelId.equals(old.getChannelId().get()))
                .flatMap(id -> old.getChannel().flatMapMany(VoiceChannel::getVoiceStates).count())
                .doOnNext(System.out::println)
                .filter(aLong -> aLong == 1L)
                .doOnNext(unused -> {
                    Mono.delay(Duration.ofSeconds(30)).then(
                            old.getGuild()
                                    .flatMap(Guild::getSelfMember)
                                    .flatMap(PartialMember::getVoiceState)
                                    .flatMap(VoiceState::getChannel)
                                    .flatMapMany(VoiceChannel::getVoiceStates)
                                    .count()
                                    .filter(aLong -> aLong == 1L)
                                    .doOnNext(aLong ->  GuildMusicManager.of(old.getGuildId()).getScheduler().stop())
                                    .flatMap(aLong -> old.getGuild())
                                    .flatMap(Guild::getVoiceConnection)
                                    .flatMap(VoiceConnection::disconnect)
                    ).subscribe();
                }).then();
    }

}
