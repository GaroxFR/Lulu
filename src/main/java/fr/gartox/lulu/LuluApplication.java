package fr.gartox.lulu;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.rest.RestClient;
import fr.gartox.lulu.listener.SlashCommandListener;
import fr.gartox.lulu.listener.VoiceStateListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class LuluApplication {
	public static void main(String[] args) {
		ApplicationContext applicationContext = SpringApplication.run(LuluApplication.class, args);

		DiscordClientBuilder.create(System.getenv("BOT_TOKEN")).build()
				.withGateway(gateway -> {
					SlashCommandListener slashCommandListener = new SlashCommandListener(applicationContext);
					VoiceStateListener voiceStateListener = new VoiceStateListener();
					// TODO Create command listener
					Mono<Void> onSlashCommandMono = gateway
							.on(ChatInputInteractionEvent.class, slashCommandListener::handle)
							.then();

					Mono<Void> onVoiceUpdateMono = gateway
							.on(VoiceStateUpdateEvent.class, voiceStateListener::handle)
							.then();
					return Mono.when(onSlashCommandMono, onVoiceUpdateMono);
				}).block();
	}

	@Bean
	public RestClient discordRestClient() {
		return RestClient.create(System.getenv("BOT_TOKEN"));
	}

}
