package ltdjms.discord.currency.services;

import ltdjms.discord.currency.domain.BalanceView;

/**
 * Service interface for retrieving member balances.
 */
public interface BalanceService {

    /**
     * Gets the balance view for a member in a guild.
     * If the member has no account, one is created with zero balance.
     * If the guild has no currency configuration, defaults are used.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @return the balance view with amount, currency name, and icon
     */
    BalanceView getBalance(long guildId, long userId);
}
