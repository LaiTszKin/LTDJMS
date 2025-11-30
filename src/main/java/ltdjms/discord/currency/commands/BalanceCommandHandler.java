package ltdjms.discord.currency.commands;

import ltdjms.discord.currency.bot.BotErrorHandler;
import ltdjms.discord.currency.bot.SlashCommandListener;
import ltdjms.discord.currency.domain.BalanceView;
import ltdjms.discord.currency.persistence.RepositoryException;
import ltdjms.discord.currency.services.BalanceService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for the /balance slash command.
 * Shows the user's current currency balance in the guild.
 */
public class BalanceCommandHandler implements SlashCommandListener.CommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BalanceCommandHandler.class);

    private final BalanceService balanceService;

    public BalanceCommandHandler(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        LOG.debug("Processing /balance for guildId={}, userId={}", guildId, userId);

        try {
            BalanceView balanceView = balanceService.getBalance(guildId, userId);

            String message = balanceView.formatMessage();
            event.reply(message).queue();

            BotErrorHandler.logSuccess(event, "balance=" + balanceView.balance());

        } catch (RepositoryException e) {
            BotErrorHandler.handleDatabaseError(event, e);
        } catch (Exception e) {
            BotErrorHandler.handleUnexpectedError(event, e);
        }
    }
}
