package ltdjms.discord.panel.services;

import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceService;
import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.gametoken.persistence.DiceGame1ConfigRepository;
import ltdjms.discord.gametoken.persistence.DiceGame2ConfigRepository;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for admin panel operations.
 * Provides methods for managing member balances, game tokens, and game settings.
 */
public class AdminPanelService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPanelService.class);

    private final BalanceService balanceService;
    private final BalanceAdjustmentService balanceAdjustmentService;
    private final GameTokenService gameTokenService;
    private final GameTokenTransactionService transactionService;
    private final DiceGame1ConfigRepository diceGame1ConfigRepository;
    private final DiceGame2ConfigRepository diceGame2ConfigRepository;

    public AdminPanelService(
            BalanceService balanceService,
            BalanceAdjustmentService balanceAdjustmentService,
            GameTokenService gameTokenService,
            GameTokenTransactionService transactionService,
            DiceGame1ConfigRepository diceGame1ConfigRepository,
            DiceGame2ConfigRepository diceGame2ConfigRepository
    ) {
        this.balanceService = balanceService;
        this.balanceAdjustmentService = balanceAdjustmentService;
        this.gameTokenService = gameTokenService;
        this.transactionService = transactionService;
        this.diceGame1ConfigRepository = diceGame1ConfigRepository;
        this.diceGame2ConfigRepository = diceGame2ConfigRepository;
    }

    /**
     * Gets a member's current currency balance.
     */
    public Result<Long, DomainError> getMemberBalance(long guildId, long userId) {
        return balanceService.tryGetBalance(guildId, userId)
                .map(view -> view.balance());
    }

    /**
     * Gets a member's current game token balance.
     */
    public long getMemberTokens(long guildId, long userId) {
        return gameTokenService.getBalance(guildId, userId);
    }

    /**
     * Adjusts a member's currency balance.
     */
    public Result<BalanceAdjustmentResult, DomainError> adjustBalance(
            long guildId, long userId, String mode, long amount) {
        LOG.debug("Admin panel adjusting balance: guildId={}, userId={}, mode={}, amount={}",
                guildId, userId, mode, amount);

        return switch (mode) {
            case "add" -> balanceAdjustmentService.tryAdjustBalance(guildId, userId, amount)
                    .map(this::toBalanceAdjustmentResult);
            case "deduct" -> balanceAdjustmentService.tryAdjustBalance(guildId, userId, -amount)
                    .map(this::toBalanceAdjustmentResult);
            case "adjust" -> balanceAdjustmentService.tryAdjustBalanceTo(guildId, userId, amount)
                    .map(this::toBalanceAdjustmentResult);
            default -> Result.err(DomainError.invalidInput("Unknown adjustment mode: " + mode));
        };
    }

    private BalanceAdjustmentResult toBalanceAdjustmentResult(
            BalanceAdjustmentService.BalanceAdjustmentResult result) {
        return new BalanceAdjustmentResult(
                result.previousBalance(),
                result.newBalance(),
                result.adjustment()
        );
    }

    /**
     * Adjusts a member's game token balance.
     */
    public Result<TokenAdjustmentResult, DomainError> adjustTokens(
            long guildId, long userId, long amount) {
        LOG.debug("Admin panel adjusting tokens: guildId={}, userId={}, amount={}",
                guildId, userId, amount);

        long previousTokens = gameTokenService.getBalance(guildId, userId);

        return gameTokenService.tryAdjustTokens(guildId, userId, amount)
                .map(result -> {
                    transactionService.recordTransaction(
                            guildId, userId, amount, result.newTokens(),
                            GameTokenTransaction.Source.ADMIN_ADJUSTMENT,
                            null);

                    return new TokenAdjustmentResult(previousTokens, result.newTokens(), amount);
                });
    }

    /**
     * Gets the current token cost for a dice game.
     */
    public long getGameTokenCost(long guildId, String gameType) {
        return switch (gameType) {
            case "dice-game-1" -> diceGame1ConfigRepository.findOrCreateDefault(guildId).tokensPerPlay();
            case "dice-game-2" -> diceGame2ConfigRepository.findOrCreateDefault(guildId).tokensPerPlay();
            default -> throw new IllegalArgumentException("Unknown game type: " + gameType);
        };
    }

    /**
     * Updates the token cost for a dice game.
     */
    public Result<GameConfigUpdateResult, DomainError> updateGameTokenCost(
            long guildId, String gameType, long newCost) {
        LOG.debug("Admin panel updating game token cost: guildId={}, gameType={}, newCost={}",
                guildId, gameType, newCost);

        if (newCost < 0) {
            return Result.err(DomainError.invalidInput("Token cost cannot be negative"));
        }

        long previousCost = getGameTokenCost(guildId, gameType);

        switch (gameType) {
            case "dice-game-1" -> diceGame1ConfigRepository.updateTokensPerPlay(guildId, newCost);
            case "dice-game-2" -> diceGame2ConfigRepository.updateTokensPerPlay(guildId, newCost);
            default -> {
                return Result.err(DomainError.invalidInput("Unknown game type: " + gameType));
            }
        }

        LOG.info("Game token cost updated: guildId={}, gameType={}, previous={}, new={}",
                guildId, gameType, previousCost, newCost);

        return Result.ok(new GameConfigUpdateResult(gameType, previousCost, newCost));
    }

    public record BalanceAdjustmentResult(long previousBalance, long newBalance, long adjustment) {
        public String formatMessage(String currencyName, String currencyIcon) {
            String action = adjustment >= 0 ? "增加" : "扣除";
            long displayAmount = Math.abs(adjustment);
            return String.format(
                    "%s %,d %s %s\n調整前：%s %,d\n調整後：%s %,d",
                    action, displayAmount, currencyIcon, currencyName,
                    currencyIcon, previousBalance,
                    currencyIcon, newBalance
            );
        }
    }

    public record TokenAdjustmentResult(long previousTokens, long newTokens, long adjustment) {
        public String formatMessage() {
            String action = adjustment >= 0 ? "增加" : "扣除";
            long displayAmount = Math.abs(adjustment);
            return String.format(
                    "%s %,d 遊戲代幣\n調整前：🎮 %,d\n調整後：🎮 %,d",
                    action, displayAmount, previousTokens, newTokens
            );
        }
    }

    public record GameConfigUpdateResult(String gameType, long previousCost, long newCost) {
        public String formatMessage() {
            String gameName = switch (gameType) {
                case "dice-game-1" -> "骰子遊戲 1";
                case "dice-game-2" -> "骰子遊戲 2";
                default -> gameType;
            };
            return String.format(
                    "已更新 %s 的遊戲代幣消耗\n調整前：🎮 %,d\n調整後：🎮 %,d",
                    gameName, previousCost, newCost
            );
        }
    }
}
