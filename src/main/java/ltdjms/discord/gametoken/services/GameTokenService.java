package ltdjms.discord.gametoken.services;

import ltdjms.discord.gametoken.domain.GameTokenAccount;
import ltdjms.discord.gametoken.persistence.GameTokenAccountRepository;
import ltdjms.discord.gametoken.persistence.InsufficientTokensException;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing game token accounts.
 * Handles token adjustments with validation for non-negative balances.
 */
public class GameTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(GameTokenService.class);

    private final GameTokenAccountRepository accountRepository;

    public GameTokenService(GameTokenAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Gets the current token balance for a member.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @return the token balance (0 if account doesn't exist)
     */
    public long getBalance(long guildId, long userId) {
        return accountRepository.findByGuildIdAndUserId(guildId, userId)
                .map(GameTokenAccount::tokens)
                .orElse(0L);
    }

    /**
     * Adjusts a member's token balance by the specified amount.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @param amount  the amount to adjust (positive for credit, negative for debit)
     * @return the adjustment result
     * @throws InsufficientTokensException if the adjustment would result in a negative balance
     */
    public TokenAdjustmentResult adjustTokens(long guildId, long userId, long amount) {
        LOG.debug("Adjusting tokens for guildId={}, userId={}, amount={}", guildId, userId, amount);

        // Get current balance
        GameTokenAccount current = accountRepository.findOrCreate(guildId, userId);
        long previousTokens = current.tokens();

        // Apply adjustment
        GameTokenAccount updated = accountRepository.adjustTokens(guildId, userId, amount);

        TokenAdjustmentResult result = new TokenAdjustmentResult(
                guildId,
                userId,
                previousTokens,
                updated.tokens(),
                amount
        );

        LOG.info("Tokens adjusted: guildId={}, userId={}, previous={}, new={}, adjustment={}",
                guildId, userId, previousTokens, updated.tokens(), amount);

        return result;
    }

    /**
     * Checks if a member has enough tokens.
     *
     * @param guildId        the Discord guild ID
     * @param userId         the Discord user ID
     * @param requiredTokens the number of tokens required
     * @return true if the member has enough tokens
     */
    public boolean hasEnoughTokens(long guildId, long userId, long requiredTokens) {
        return getBalance(guildId, userId) >= requiredTokens;
    }

    /**
     * Deducts tokens from a member's account.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @param tokens  the number of tokens to deduct (must be positive)
     * @return the updated account
     * @throws IllegalArgumentException    if tokens is not positive
     * @throws InsufficientTokensException if the member doesn't have enough tokens
     */
    public GameTokenAccount deductTokens(long guildId, long userId, long tokens) {
        if (tokens <= 0) {
            throw new IllegalArgumentException("Tokens to deduct must be positive: " + tokens);
        }
        return accountRepository.adjustTokens(guildId, userId, -tokens);
    }

    /**
     * Adjusts a member's token balance using Result-based error handling.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @param amount  the amount to adjust (positive for credit, negative for debit)
     * @return Result containing TokenAdjustmentResult on success, or DomainError on failure
     */
    public Result<TokenAdjustmentResult, DomainError> tryAdjustTokens(long guildId, long userId, long amount) {
        LOG.debug("Adjusting tokens for guildId={}, userId={}, amount={}", guildId, userId, amount);

        // Get current balance
        GameTokenAccount current = accountRepository.findOrCreate(guildId, userId);
        long previousTokens = current.tokens();

        // Apply adjustment using Result-based API
        Result<GameTokenAccount, DomainError> adjustResult =
                accountRepository.tryAdjustTokens(guildId, userId, amount);

        if (adjustResult.isErr()) {
            return Result.err(adjustResult.getError());
        }

        GameTokenAccount updated = adjustResult.getValue();

        TokenAdjustmentResult result = new TokenAdjustmentResult(
                guildId,
                userId,
                previousTokens,
                updated.tokens(),
                amount
        );

        LOG.info("Tokens adjusted: guildId={}, userId={}, previous={}, new={}, adjustment={}",
                guildId, userId, previousTokens, updated.tokens(), amount);

        return Result.ok(result);
    }

    /**
     * Deducts tokens using Result-based error handling.
     *
     * @param guildId the Discord guild ID
     * @param userId  the Discord user ID
     * @param tokens  the number of tokens to deduct (must be positive)
     * @return Result containing the updated account, or DomainError on failure
     */
    public Result<GameTokenAccount, DomainError> tryDeductTokens(long guildId, long userId, long tokens) {
        if (tokens <= 0) {
            return Result.err(DomainError.invalidInput("Tokens to deduct must be positive: " + tokens));
        }
        return accountRepository.tryAdjustTokens(guildId, userId, -tokens);
    }

    /**
     * Result of a token adjustment operation.
     */
    public record TokenAdjustmentResult(
            long guildId,
            long userId,
            long previousTokens,
            long newTokens,
            long adjustment
    ) {
        /**
         * Formats the result as a Discord message.
         */
        public String formatMessage(String targetUserMention) {
            String action = adjustment >= 0 ? "Added" : "Removed";
            long displayAmount = Math.abs(adjustment);
            return String.format("%s %,d game tokens to/from %s\nNew balance: %,d game tokens",
                    action, displayAmount, targetUserMention, newTokens);
        }
    }
}
