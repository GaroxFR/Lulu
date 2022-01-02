package fr.gartox.lulu.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.VoiceConnection;
import fr.gartox.lulu.music.AudioTrackScheduler;
import fr.gartox.lulu.music.GuildMusicManager;
import fr.gartox.lulu.music.SearchResult;
import fr.gartox.lulu.utils.Utils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.DurationFormat;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Component
public class PlayCommand implements SlashCommand {
    @Override
    public String getName() {
        return "play";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.deferReply()
                .then(this.join(event))
                .filter(Boolean::booleanValue)
                .then(this.play(event));
    }

    private Mono<Void> play(ChatInputInteractionEvent event) {
        String music = event.getOption("musique")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        if (music.startsWith("http")) {
            return playFromLink(event, music);
        } else {
            return researchAndPlay(event, music);
        }


    }

    private Mono<Void> researchAndPlay(ChatInputInteractionEvent event, String music) {
        GuildMusicManager guildMusicManager = GuildMusicManager.of(event.getInteraction().getGuildId().get());
        AudioTrackScheduler scheduler = guildMusicManager.getScheduler();

        return Mono.<SearchResult>create(sink -> {
            GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(guildMusicManager, "ytsearch:" + music, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    sink.success(SearchResult.of(track));
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    sink.success(SearchResult.of(playlist));
                }

                @Override
                public void noMatches() {
                    sink.success(SearchResult.error("Aucune vidéo trouvée"));
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    sink.success(SearchResult.error(exception.getMessage()));
                }
            });
        }).flatMap(searchResult -> {
            switch (searchResult.getType()) {
                case TRACK:
                    scheduler.play(searchResult.getTrack());
                    return event.createFollowup(searchResult.getTrack().getInfo().title + " a été ajouté à la queue").then();
                case PLAYLIST:
                    Map<String, AudioTrack> trackMap = new HashMap<>();
                    Button[] buttons = new Button[5];
                    EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                            .title("Résultat de votre recherche")
                            .color(Color.GREEN)
                            .timestamp(Instant.now())
                            .author(EmbedCreateFields.Author.of(event.getInteraction().getUser().getUsername(), null, event.getInteraction().getUser().getAvatarUrl()));
                    for (int i = 0; i < Math.min(5, searchResult.getPlaylist().getTracks().size()); i++) {
                        AudioTrack track = searchResult.getPlaylist().getTracks().get(i);
                        trackMap.put(track.getIdentifier() + event.getInteraction().getId(), track);
                        buttons[i] = Button.primary(track.getIdentifier() + event.getInteraction().getId(), String.valueOf(i+1));
                        embedBuilder.addField((i+1) + ". " + track.getInfo().title, "(" + Utils.formatTime(track.getInfo().length) + ")", false);
                    }
                    return event.createFollowup()
                            .withEmbeds(embedBuilder.build())
                            .withComponents(ActionRow.of(buttons))
                            .flatMap(message -> {
                                Mono<Void> tempListener = event.getClient().on(ButtonInteractionEvent.class, buttonInteractionEvent -> {
                                    if (trackMap.containsKey(buttonInteractionEvent.getCustomId())) {
                                        AudioTrack track = trackMap.get(buttonInteractionEvent.getCustomId());
                                        scheduler.play(track);
                                        return event.editFollowup(message.getId()).withContentOrNull("Vous avez sélectionné " + track.getInfo().title).withEmbeds().withComponents()
                                                .flatMap(message1 -> buttonInteractionEvent.reply().withContent(track.getInfo().title + " a été ajouté à la queue"));
                                    } else {
                                        return Mono.empty();
                                    }
                                }).then();

                                Mono<Void> timeoutMono = Mono.delay(Duration.ofMinutes(3))
                                        .flatMap(aLong -> event.deleteFollowup(message.getId())).then();
                                return Mono.firstWithSignal(tempListener, timeoutMono);
                            });
                default:
                    return event.createFollowup(searchResult.getErrorMessage()).then();

            }
        });
    }

    private Mono<Void> playFromLink(ChatInputInteractionEvent event, String music) {
        GuildMusicManager guildMusicManager = GuildMusicManager.of(event.getInteraction().getGuildId().get());
        AudioTrackScheduler scheduler = guildMusicManager.getScheduler();
        return Mono.<String>create(monoSink -> {
            GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(guildMusicManager, music, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    scheduler.play(track);
                    monoSink.success(track.getInfo().title + " a été ajouté queue");
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    for (AudioTrack track : playlist.getTracks()) {
                        scheduler.play(track);
                    }
                    monoSink.success("La playlist" + playlist.getName() + " a été ajouté à la queue");
                }

                @Override
                public void noMatches() {
                    monoSink.success("Musique introuvable.");
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    monoSink.success(exception.getMessage());
                }
            });
        }).flatMap(message -> event.createFollowup(message).then());
    }

    private Mono<Boolean> join(ChatInputInteractionEvent event) {
        Optional<Member> memberOptional = event.getInteraction().getMember();
        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            GuildMusicManager guildMusicManager = GuildMusicManager.of(event.getInteraction().getGuildId().get());
            return event.getInteraction().getGuild().flatMap(guild -> guild.getVoiceConnection())
                    .switchIfEmpty(member.getVoiceState()
                            .flatMap(VoiceState::getChannel)
                            .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(guildMusicManager.getProvider()))))
                    .then(Mono.just(true));
        }

        return event.createFollowup("Une erreur est surevenue, impossible de récupérer la source de la commande.").then(Mono.just(false));
    }

}
