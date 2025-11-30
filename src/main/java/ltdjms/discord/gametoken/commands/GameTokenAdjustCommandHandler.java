package ltdjms.discord.gametoken.commands;

import ltdjms.discord.currency.bot.BotErrorHandler;
import ltdjms.discord.currency.bot.SlashCommandListener;
import ltdjms.discord.currency.persistence.RepositoryException;
import ltdjms.discord.gametoken.persistence.InsufficientTokensException;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenService.TokenAdjustmentResult;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the /game-token-adjust slash command.
 * Allows administrators to add or subtract game tokens from a member's account.
 */
public class GameTokenAdjustCommandHandler implements SlashCommandListener.CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GameTokenAdjustCommandHandler.class);

    private final GameTokenService tokenService;

    public GameTokenAdjustCommandHandler(GameTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        LOG.debug("Processing /game-token-adjust for guildId={}", guildId);

        // Get required options
        OptionMapping memberOption = event.getOption("member");
        OptionMapping amountOption = event.getOption("amount");

        if (memberOption == null || amountOption == null) {
            BotErrorHandler.handleInvalidInput(event, "Both member and amount are required.");
            return;
        }

        User targetUser = memberOption.getAsUser();
        long targetUserId = targetUser.getIdLong();
        long amount = amountOption.getAsLong();

        if (amount == 0) {
            BotErrorHandler.handleInvalidInput(event, "Amount cannot be zero.");
            return;
        }

        try {
            TokenAdjustmentResult result = tokenService.adjustTokens(guildId, targetUserId, amount);

            String message = result.formatMessage(targetUser.getAsMention());
            event.reply(message).queue();

            BotErrorHandler.logSuccess(event,
                    "user=" + targetUserId + ", amount=" + amount + ", newTokens=" + result.newTokens());

        } catch (IllegalArgumentException e) {
            BotErrorHandler.handleInvalidInput(event, e.getMessage());
        } catch (InsufficientTokensException e) {
            BotErrorHandler.handleInvalidInput(event,
                    "Cannot reduce tokens below zero. Current balance is insufficient for this deduction.");
        } catch (RepositoryException e) {
            BotErrorHandler.handleDatabaseError(event, e);
        } catch (Exception e) {
            BotErrorHandler.handleUnexpectedError(event, e);
        }
    }
}
