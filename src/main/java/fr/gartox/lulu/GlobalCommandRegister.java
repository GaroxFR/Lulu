package fr.gartox.lulu;

import discord4j.common.JacksonResources;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class GlobalCommandRegister implements ApplicationRunner {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient client;
    private final long GUILD_ID = 832025499772911706L;

    public GlobalCommandRegister(RestClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final JacksonResources mapper = JacksonResources.create();

        final long applicationId = this.client.getApplicationId().block();

        Map<String, ApplicationCommandData> discordCommands = this.client.getApplicationService()
                .getGuildApplicationCommands(applicationId, this.GUILD_ID)
                .collectMap(ApplicationCommandData::name)
                .switchIfEmpty(Mono.just(new HashMap<>()))
                .block();

        Map<String, ApplicationCommandRequest> commands = new HashMap<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("commands/*.json")) {
            ApplicationCommandRequest request = mapper.getObjectMapper().readValue(resource.getInputStream(), ApplicationCommandRequest.class);
            commands.put(request.name(), request);

            if (!discordCommands.containsKey(request.name())) {
                this.client.getApplicationService().createGuildApplicationCommand(applicationId, this.GUILD_ID, request).block();

                this.LOGGER.info("Created global command: " + request.name());
            }
        }

        for (ApplicationCommandData discordCommand : discordCommands.values()) {
            long discordCommandId = Long.parseLong(discordCommand.id());

            ApplicationCommandRequest command = commands.get(discordCommand.name());

            if (command == null) {
                //Removed command.json, delete global command
                this.client.getApplicationService().deleteGuildApplicationCommand(applicationId,this.GUILD_ID, discordCommandId).block();

                this.LOGGER.info("Deleted global command: " + discordCommand.name());
                continue; //Skip further processing on this command.
            }

            if (this.hasChanged(discordCommand, command)) {
                this.client.getApplicationService().modifyGuildApplicationCommand(applicationId, this.GUILD_ID,discordCommandId, command).block();

                this.LOGGER.info("Updated global command: " + command.name());
            }
        }
    }

    private boolean hasChanged(ApplicationCommandData discordCommand, ApplicationCommandRequest command) {
        // Compare types
        if (!discordCommand.type().toOptional().orElse(1).equals(command.type().toOptional().orElse(1)))
            return true;

        //Check if description has changed.
        if (!discordCommand.description().equals(command.description().toOptional().orElse("")))
            return true;

        //Check if default permissions have changed
        boolean discordCommandDefaultPermission = discordCommand.defaultPermission().toOptional().orElse(true);
        boolean commandDefaultPermission = command.defaultPermission().toOptional().orElse(true);

        if (discordCommandDefaultPermission != commandDefaultPermission)
            return true;

        //Check and return if options have changed.
        return !discordCommand.options().equals(command.options());
    }
}