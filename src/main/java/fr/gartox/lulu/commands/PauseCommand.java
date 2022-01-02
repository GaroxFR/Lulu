package fr.gartox.lulu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import fr.gartox.lulu.music.GuildMusicManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PauseCommand implements SlashCommand {
    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GuildMusicManager.of(event.getInteraction().getGuildId().get()).getScheduler().pause();
        return event.reply("Vous avez mis pause");

    }
}
