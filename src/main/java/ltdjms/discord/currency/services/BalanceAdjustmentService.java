package ltdjms.discord.currency.services;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.currency.persistence.NegativeBalanceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for adjusting member currency balances.
 * Handles atomic balance changes with validation for non-negative balances.
 */
public class BalanceAdjustmentService {

    private static final Logger LOG = LoggerFactory.getLogger(BalanceAdjustmentService.class);

    private final MemberCurrencyAccountRepository accountRepository;
    private final GuildCurrencyConfigRepository configRepository;

    public BalanceAdjustmentService(
            MemberCurrencyAccountRepository accountRepository,
            GuildCurrencyConfigRepository configRepository) {
        this.accountRepository = accountRepository;
        this.configRepository = configRepository;
    }

    /**
     * Adjusts a member's balance by the specified amount.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @param amount  the amount to adjust (positive for credit, negative for debit)
     * @return the adjustment result
     * @throws IllegalArgumentException   if the amount exceeds the per-command maximum
     * @throws NegativeBalanceException   if the adjustment would result in a negative balance
     */
    public BalanceAdjustmentResult adjustBalance(long guildId, long userId, long amount) {
        LOG.debug("Adjusting balance for guildId={}, userId={}, amount={}", guildId, userId, amount);

        // Validate amount
        if (!MemberCurrencyAccount.isValidAdjustmentAmount(amount)) {
            throw new IllegalArgumentException(
                    "Amount exceeds maximum: |" + amount + "| > " + MemberCurrencyAccount.MAX_ADJUSTMENT_AMOUNT);
        }

        // Get current balance
        MemberCurrencyAccount current = accountRepository.findOrCreate(guildId, userId);
        long previousBalance = current.balance();

        // Apply adjustment
        MemberCurrencyAccount updated = accountRepository.adjustBalance(guildId, userId, amount);

        // Get currency config for display
        GuildCurrencyConfig config = configRepository.findByGuildId(guildId)
                .orElse(GuildCurrencyConfig.createDefault(guildId));

        BalanceAdjustmentResult result = new BalanceAdjustmentResult(
                guildId,
                userId,
                previousBalance,
                updated.balance(),
                amount,
                config.currencyName(),
                config.currencyIcon()
        );

        LOG.info("Balance adjusted: guildId={}, userId={}, previous={}, new={}, adjustment={}",
                guildId, userId, previousBalance, updated.balance(), amount);

        return result;
    }

    /**
     * Result of a balance adjustment operation.
     */
    public record BalanceAdjustmentResult(
            long guildId,
            long userId,
            long previousBalance,
            long newBalance,
            long adjustment,
            String currencyName,
            String currencyIcon
    ) {
        /**
         * Formats the result as a Discord message.
         */
        public String formatMessage(String targetUserMention) {
            String action = adjustment >= 0 ? "Added" : "Removed";
            long displayAmount = Math.abs(adjustment);
            return String.format("%s %s %,d %s to/from %s\nNew balance: %s %,d %s",
                    action, currencyIcon, displayAmount, currencyName, targetUserMention,
                    currencyIcon, newBalance, currencyName);
        }
    }
}
