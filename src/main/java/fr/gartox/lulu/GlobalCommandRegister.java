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

    public GlobalCommandRegister(RestClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final JacksonResources mapper = JacksonResources.create();

        final long applicationId = this.client.getApplicationId().block();

        Map<String, ApplicationCommandData> discordCommands = this.client.getApplicationService()
                .getGlobalApplicationCommands(applicationId)
                .collectMap(ApplicationCommandData::name)
                .switchIfEmpty(Mono.just(new HashMap<>()))
                .block();

        Map<String, ApplicationCommandRequest> commands = new HashMap<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("commands/*.json")) {
            ApplicationCommandRequest request = mapper.getObjectMapper().readValue(resource.getInputStream(), ApplicationCommandRequest.class);
            commands.put(request.name(), request);

            if (!discordCommands.containsKey(request.name())) {
                this.client.getApplicationService().createGlobalApplicationCommand(applicationId, request).block();

                this.LOGGER.info("Created global command: " + request.name());
            }
        }

        for (ApplicationCommandData discordCommand : discordCommands.values()) {
            long discordCommandId = discordCommand.id().asLong();

            ApplicationCommandRequest command = commands.get(discordCommand.name());

            if (command == null) {
                //Removed command.json, delete global command
                this.client.getApplicationService().deleteGlobalApplicationCommand(applicationId, discordCommandId).block();

                this.LOGGER.info("Deleted global command: " + discordCommand.name());
                continue; //Skip further processing on this command.
            }

            if (this.hasChanged(discordCommand, command)) {
                this.client.getApplicationService().modifyGlobalApplicationCommand(applicationId,discordCommandId, command).block();

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