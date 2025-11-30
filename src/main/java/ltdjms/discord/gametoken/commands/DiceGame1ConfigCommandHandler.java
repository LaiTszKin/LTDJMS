package ltdjms.discord.gametoken.commands;

import ltdjms.discord.currency.bot.BotErrorHandler;
import ltdjms.discord.currency.bot.SlashCommandListener;
import ltdjms.discord.currency.persistence.RepositoryException;
import ltdjms.discord.gametoken.domain.DiceGame1Config;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the /dice-game-1-config slash command.
 * Allows administrators to configure the token cost for playing dice-game-1.
 */
public class DiceGame1ConfigCommandHandler implements SlashCommandListener.CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DiceGame1ConfigCommandHandler.class);

    private final DiceGame1ConfigRepository configRepository;

    public DiceGame1ConfigCommandHandler(DiceGame1ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        LOG.debug("Processing /dice-game-1-config for guildId={}", guildId);

        // Get the token-cost option
        OptionMapping tokenCostOption = event.getOption("token-cost");

        if (tokenCostOption == null) {
            // Show current configuration
            showCurrentConfig(event, guildId);
            return;
        }

        long tokenCost = tokenCostOption.getAsLong();

        if (tokenCost < 0) {
            BotErrorHandler.handleInvalidInput(event, "Token cost cannot be negative.");
            return;
        }

        try {
            DiceGame1Config updated = configRepository.updateTokensPerPlay(guildId, tokenCost);

            String message = String.format(
                    "Dice game configuration updated!\n" +
                            "Tokens required per play: %,d",
                    updated.tokensPerPlay()
            );
            event.reply(message).queue();

            BotErrorHandler.logSuccess(event, "tokensPerPlay=" + updated.tokensPerPlay());

        } catch (IllegalArgumentException e) {
            BotErrorHandler.handleInvalidInput(event, e.getMessage());
        } catch (RepositoryException e) {
            BotErrorHandler.handleDatabaseError(event, e);
        } catch (Exception e) {
            BotErrorHandler.handleUnexpectedError(event, e);
        }
    }

    private void showCurrentConfig(SlashCommandInteractionEvent event, long guildId) {
        try {
            DiceGame1Config config = configRepository.findOrCreateDefault(guildId);

            String message = String.format(
                    "**Dice Game Configuration**\n" +
                            "Tokens required per play: %,d\n\n" +
                            "Use `/dice-game-1-config token-cost:<amount>` to change the setting.",
                    config.tokensPerPlay()
            );
            event.reply(message).queue();

        } catch (RepositoryException e) {
            BotErrorHandler.handleDatabaseError(event, e);
        } catch (Exception e) {
            BotErrorHandler.handleUnexpectedError(event, e);
        }
    }
}
