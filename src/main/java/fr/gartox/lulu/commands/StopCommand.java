package fr.gartox.lulu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.voice.VoiceConnection;
import fr.gartox.lulu.music.GuildMusicManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StopCommand implements SlashCommand{
    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply().then(this.stop(event));
    }

    private Mono<Void> stop(ChatInputInteractionEvent event) {
        GuildMusicManager.of(event.getInteraction().getGuildId().get()).getScheduler().stop();
        return event.getInteraction().getGuild()
                .flatMap(Guild::getVoiceConnection)
                .flatMap(VoiceConnection::disconnect)
                .then(event.createFollowup("Vous avez vidé la queue et déconnecté le bot.").then());
    }
}
