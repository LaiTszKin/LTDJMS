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
 * Allows administrators to add, subtract, or set a member's balance to a specific value.
 * Supports three modes: add, deduct, and adjust.
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
        OptionMapping modeOption = event.getOption("mode");
        OptionMapping memberOption = event.getOption("member");
        OptionMapping amountOption = event.getOption("amount");

        if (modeOption == null || memberOption == null || amountOption == null) {
            BotErrorHandler.handleInvalidInput(event, "Mode, member, and amount are all required.");
            return;
        }

        String mode = modeOption.getAsString();
        User targetUser = memberOption.getAsUser();
        long targetUserId = targetUser.getIdLong();
        long amount = amountOption.getAsLong();

        try {
            BalanceAdjustmentResult result = switch (mode) {
                case "add" -> handleAddMode(guildId, targetUserId, amount);
                case "deduct" -> handleDeductMode(guildId, targetUserId, amount);
                case "adjust" -> handleAdjustMode(guildId, targetUserId, amount);
                default -> {
                    BotErrorHandler.handleInvalidInput(event, "Invalid mode: " + mode);
                    yield null;
                }
            };

            if (result != null) {
                String message = formatResultMessage(mode, result, targetUser.getAsMention());
                event.reply(message).queue();

                BotErrorHandler.logSuccess(event,
                        "mode=" + mode + ", user=" + targetUserId +
                        ", amount=" + amount + ", newBalance=" + result.newBalance());
            }

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

    private BalanceAdjustmentResult handleAddMode(long guildId, long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive for add mode.");
        }
        return adjustmentService.adjustBalance(guildId, userId, amount);
    }

    private BalanceAdjustmentResult handleDeductMode(long guildId, long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive for deduct mode.");
        }
        return adjustmentService.adjustBalance(guildId, userId, -amount);
    }

    private BalanceAdjustmentResult handleAdjustMode(long guildId, long userId, long targetBalance) {
        if (targetBalance < 0) {
            throw new IllegalArgumentException("Target balance cannot be negative for adjust mode.");
        }
        return adjustmentService.adjustBalanceTo(guildId, userId, targetBalance);
    }

    private String formatResultMessage(String mode, BalanceAdjustmentResult result, String targetUserMention) {
        return switch (mode) {
            case "add" -> String.format("Added %s %,d %s to %s\nNew balance: %s %,d %s",
                    result.currencyIcon(), result.adjustment(), result.currencyName(), targetUserMention,
                    result.currencyIcon(), result.newBalance(), result.currencyName());
            case "deduct" -> String.format("Removed %s %,d %s from %s\nNew balance: %s %,d %s",
                    result.currencyIcon(), Math.abs(result.adjustment()), result.currencyName(), targetUserMention,
                    result.currencyIcon(), result.newBalance(), result.currencyName());
            case "adjust" -> String.format("Adjusted %s balance from %s %,d to %s %,d %s (adjustment: %+d)\nNew balance: %s %,d %s",
                    targetUserMention,
                    result.currencyIcon(), result.previousBalance(),
                    result.currencyIcon(), result.newBalance(), result.currencyName(),
                    result.adjustment(),
                    result.currencyIcon(), result.newBalance(), result.currencyName());
            default -> result.formatMessage(targetUserMention);
        };
    }
}
