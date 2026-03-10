package ltdjms.discord.product.services;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.CurrencyTransactionService;
import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** Centralizes automatic product reward fulfillment across purchase and redemption flows. */
public class ProductRewardService {

  private final BalanceAdjustmentService balanceAdjustmentService;
  private final GameTokenService gameTokenService;
  private final CurrencyTransactionService currencyTransactionService;
  private final GameTokenTransactionService gameTokenTransactionService;

  public ProductRewardService(
      BalanceAdjustmentService balanceAdjustmentService,
      GameTokenService gameTokenService,
      CurrencyTransactionService currencyTransactionService,
      GameTokenTransactionService gameTokenTransactionService) {
    this.balanceAdjustmentService = balanceAdjustmentService;
    this.gameTokenService = gameTokenService;
    this.currencyTransactionService = currencyTransactionService;
    this.gameTokenTransactionService = gameTokenTransactionService;
  }

  public Result<RewardGrantResult, DomainError> grantReward(RewardGrantRequest request) {
    Product product = request.product();
    if (!product.hasReward()) {
      return Result.err(DomainError.invalidInput("商品沒有自動獎勵"));
    }
    if (request.amount() <= 0) {
      return Result.err(DomainError.invalidInput("商品獎勵金額無效"));
    }

    return switch (product.rewardType()) {
      case CURRENCY -> grantCurrencyReward(request);
      case TOKEN -> grantTokenReward(request);
    };
  }

  private Result<RewardGrantResult, DomainError> grantCurrencyReward(RewardGrantRequest request) {
    if (request.currencySource() == null) {
      return Result.err(DomainError.unexpectedFailure("缺少貨幣獎勵交易來源", null));
    }

    var adjustResult =
        balanceAdjustmentService.tryAdjustBalance(
            request.guildId(), request.userId(), request.amount());
    if (adjustResult.isErr()) {
      return Result.err(adjustResult.getError());
    }

    currencyTransactionService.recordTransaction(
        request.guildId(),
        request.userId(),
        request.amount(),
        adjustResult.getValue().newBalance(),
        request.currencySource(),
        request.description());

    return Result.ok(
        new RewardGrantResult(request.amount(), adjustResult.getValue().newBalance(), null));
  }

  private Result<RewardGrantResult, DomainError> grantTokenReward(RewardGrantRequest request) {
    if (request.tokenSource() == null) {
      return Result.err(DomainError.unexpectedFailure("缺少代幣獎勵交易來源", null));
    }

    var adjustResult =
        gameTokenService.tryAdjustTokens(request.guildId(), request.userId(), request.amount());
    if (adjustResult.isErr()) {
      return Result.err(adjustResult.getError());
    }

    gameTokenTransactionService.recordTransaction(
        request.guildId(),
        request.userId(),
        request.amount(),
        adjustResult.getValue().newTokens(),
        request.tokenSource(),
        request.description());

    return Result.ok(
        new RewardGrantResult(request.amount(), null, adjustResult.getValue().newTokens()));
  }

  public record RewardGrantRequest(
      long guildId,
      long userId,
      Product product,
      long amount,
      String description,
      CurrencyTransaction.Source currencySource,
      GameTokenTransaction.Source tokenSource) {}

  public record RewardGrantResult(long amount, Long currencyBalanceAfter, Long tokenBalanceAfter) {
    public String formatReward(Product product) {
      return switch (product.rewardType()) {
        case CURRENCY -> String.format("%,d 貨幣", amount);
        case TOKEN -> String.format("%,d 代幣", amount);
      };
    }
  }
}
