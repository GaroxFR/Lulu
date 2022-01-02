package fr.gartox.lulu.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public interface SlashCommand {

    String getName();

    Mono<Void> handle(ChatInputInteractionEvent event);
}
