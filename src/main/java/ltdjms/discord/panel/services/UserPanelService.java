package ltdjms.discord.panel.services;

import ltdjms.discord.currency.domain.BalanceView;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService.TransactionPage;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for generating user panel data.
 * Aggregates currency balance and game token information for display
 * in the personal panel embed.
 */
public class UserPanelService {

    private static final Logger LOG = LoggerFactory.getLogger(UserPanelService.class);

    private final BalanceService balanceService;
    private final GameTokenService gameTokenService;
    private final GameTokenTransactionService transactionService;

    public UserPanelService(
            BalanceService balanceService,
            GameTokenService gameTokenService,
            GameTokenTransactionService transactionService
    ) {
        this.balanceService = balanceService;
        this.gameTokenService = gameTokenService;
        this.transactionService = transactionService;
    }

    /**
     * Gets the user panel view for a member in a guild.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @return Result containing UserPanelView on success, or DomainError on failure
     */
    public Result<UserPanelView, DomainError> getUserPanelView(long guildId, long userId) {
        LOG.debug("Getting user panel view for guildId={}, userId={}", guildId, userId);

        // Get currency balance
        Result<BalanceView, DomainError> balanceResult = balanceService.tryGetBalance(guildId, userId);
        if (balanceResult.isErr()) {
            LOG.warn("Failed to get balance for guildId={}, userId={}: {}",
                    guildId, userId, balanceResult.getError().message());
            return Result.err(balanceResult.getError());
        }

        BalanceView balanceView = balanceResult.getValue();

        // Get game token balance (this doesn't fail, returns 0 if not found)
        long gameTokens = gameTokenService.getBalance(guildId, userId);

        UserPanelView panelView = new UserPanelView(
                guildId,
                userId,
                balanceView.balance(),
                balanceView.currencyName(),
                balanceView.currencyIcon(),
                gameTokens
        );

        LOG.debug("User panel view created: guildId={}, userId={}, currency={}, tokens={}",
                guildId, userId, balanceView.balance(), gameTokens);

        return Result.ok(panelView);
    }

    /**
     * Gets a page of token transaction history for a user.
     *
     * @param guildId  the Discord guild ID
     * @param userId   the Discord user ID
     * @param page     the page number (1-based)
     * @return the transaction page
     */
    public TransactionPage getTokenTransactionPage(long guildId, long userId, int page) {
        LOG.debug("Getting token transaction page for guildId={}, userId={}, page={}", guildId, userId, page);
        return transactionService.getTransactionPage(guildId, userId, page,
                GameTokenTransactionService.DEFAULT_PAGE_SIZE);
    }
}
