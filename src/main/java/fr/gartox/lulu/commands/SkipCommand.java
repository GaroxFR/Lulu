package fr.gartox.lulu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import fr.gartox.lulu.music.GuildMusicManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SkipCommand implements SlashCommand {
    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GuildMusicManager.of(event.getInteraction().getGuildId().get()).getScheduler().skip();
        return event.reply().withContent("Vous avez skip la musique actuelle.");
    }
}
