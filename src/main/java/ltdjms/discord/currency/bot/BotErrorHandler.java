package ltdjms.discord.currency.bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for handling errors in bot interactions.
 * Provides consistent error logging and user-friendly error messages.
 */
public final class BotErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BotErrorHandler.class);

    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";
    private static final String PERMISSION_ERROR_MESSAGE = "You don't have permission to perform this action.";
    private static final String INVALID_INPUT_MESSAGE = "Invalid input. Please check your command and try again.";

    private BotErrorHandler() {
        // Utility class
    }

    /**
     * Handles unexpected exceptions by logging them and sending a user-friendly message.
     *
     * @param event the slash command event
     * @param error the exception that occurred
     */
    public static void handleUnexpectedError(SlashCommandInteractionEvent event, Throwable error) {
        LOG.error("Unexpected error in command {} for user={} in guild={}",
                event.getName(),
                event.getUser().getIdLong(),
                event.getGuild() != null ? event.getGuild().getIdLong() : "DM",
                error);

        replyWithError(event, GENERIC_ERROR_MESSAGE);
    }

    /**
     * Handles permission-related errors.
     *
     * @param event the slash command event
     */
    public static void handlePermissionError(SlashCommandInteractionEvent event) {
        LOG.warn("Permission denied for command {} for user={} in guild={}",
                event.getName(),
                event.getUser().getIdLong(),
                event.getGuild() != null ? event.getGuild().getIdLong() : "DM");

        replyWithError(event, PERMISSION_ERROR_MESSAGE);
    }

    /**
     * Handles invalid input errors with a custom message.
     *
     * @param event   the slash command event
     * @param message a user-friendly message describing the problem
     */
    public static void handleInvalidInput(SlashCommandInteractionEvent event, String message) {
        LOG.warn("Invalid input for command {} for user={} in guild={}: {}",
                event.getName(),
                event.getUser().getIdLong(),
                event.getGuild() != null ? event.getGuild().getIdLong() : "DM",
                message);

        replyWithError(event, message != null ? message : INVALID_INPUT_MESSAGE);
    }

    /**
     * Handles database-related errors.
     *
     * @param event the slash command event
     * @param error the database exception
     */
    public static void handleDatabaseError(SlashCommandInteractionEvent event, Throwable error) {
        LOG.error("Database error in command {} for user={} in guild={}",
                event.getName(),
                event.getUser().getIdLong(),
                event.getGuild() != null ? event.getGuild().getIdLong() : "DM",
                error);

        replyWithError(event, "A database error occurred. Please try again later.");
    }

    /**
     * Sends an ephemeral error reply to the user.
     *
     * @param event   the slash command event
     * @param message the error message to display
     */
    public static void replyWithError(SlashCommandInteractionEvent event, String message) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("❌ " + message).setEphemeral(true).queue(
                    success -> {},
                    failure -> LOG.error("Failed to send error hook message", failure)
            );
        } else {
            event.reply("❌ " + message).setEphemeral(true).queue(
                    success -> {},
                    failure -> LOG.error("Failed to send error reply", failure)
            );
        }
    }

    /**
     * Logs a successful command execution.
     *
     * @param event   the slash command event
     * @param details additional details about the operation
     */
    public static void logSuccess(SlashCommandInteractionEvent event, String details) {
        LOG.info("Command {} executed successfully for user={} in guild={}: {}",
                event.getName(),
                event.getUser().getIdLong(),
                event.getGuild() != null ? event.getGuild().getIdLong() : "DM",
                details);
    }
}
