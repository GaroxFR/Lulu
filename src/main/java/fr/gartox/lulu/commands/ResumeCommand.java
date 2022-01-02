package fr.gartox.lulu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import fr.gartox.lulu.music.GuildMusicManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ResumeCommand implements SlashCommand {
    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        GuildMusicManager.of(event.getInteraction().getGuildId().get()).getScheduler().resume();
        return event.reply("Vous avez repris la lecture");
    }
}
