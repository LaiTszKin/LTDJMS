package ltdjms.discord.currency.commands;

import ltdjms.discord.currency.bot.BotErrorHandler;
import ltdjms.discord.currency.bot.SlashCommandListener;
import ltdjms.discord.currency.persistence.NegativeBalanceException;
import ltdjms.discord.currency.persistence.RepositoryException;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceAdjustmentService.BalanceAdjustmentResult;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the /adjust-balance slash command.
 * Allows administrators to add or subtract currency from a member's balance.
 */
public class BalanceAdjustmentCommandHandler implements SlashCommandListener.CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BalanceAdjustmentCommandHandler.class);

    private final BalanceAdjustmentService adjustmentService;

    public BalanceAdjustmentCommandHandler(BalanceAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();

        LOG.debug("Processing /adjust-balance for guildId={}", guildId);

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
            BalanceAdjustmentResult result = adjustmentService.adjustBalance(guildId, targetUserId, amount);

            String message = result.formatMessage(targetUser.getAsMention());
            event.reply(message).queue();

            BotErrorHandler.logSuccess(event,
                    "user=" + targetUserId + ", amount=" + amount + ", newBalance=" + result.newBalance());

        } catch (IllegalArgumentException e) {
            BotErrorHandler.handleInvalidInput(event, e.getMessage());
        } catch (NegativeBalanceException e) {
            BotErrorHandler.handleInvalidInput(event,
                    "Cannot reduce balance below zero. Current balance is insufficient for this deduction.");
        } catch (RepositoryException e) {
            BotErrorHandler.handleDatabaseError(event, e);
        } catch (Exception e) {
            BotErrorHandler.handleUnexpectedError(event, e);
        }
    }
}
