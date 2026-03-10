package ltdjms.discord.product.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.CurrencyTransactionService;
import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.gametoken.services.GameTokenService;
import ltdjms.discord.gametoken.services.GameTokenTransactionService;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.shared.Result;

@ExtendWith(MockitoExtension.class)
class ProductRewardServiceTest {

  private static final long TEST_GUILD_ID = 123456789012345678L;
  private static final long TEST_USER_ID = 987654321098765432L;

  @Mock private BalanceAdjustmentService balanceAdjustmentService;
  @Mock private GameTokenService gameTokenService;
  @Mock private CurrencyTransactionService currencyTransactionService;
  @Mock private GameTokenTransactionService gameTokenTransactionService;

  private ProductRewardService productRewardService;

  @BeforeEach
  void setUp() {
    productRewardService =
        new ProductRewardService(
            balanceAdjustmentService,
            gameTokenService,
            currencyTransactionService,
            gameTokenTransactionService);
  }

  @Test
  @DisplayName("should grant currency reward and record transaction")
  void shouldGrantCurrencyRewardAndRecordTransaction() {
    Product product =
        new Product(
            1L,
            TEST_GUILD_ID,
            "禮包",
            null,
            Product.RewardType.CURRENCY,
            100L,
            null,
            Instant.now(),
            Instant.now());
    when(balanceAdjustmentService.tryAdjustBalance(TEST_GUILD_ID, TEST_USER_ID, 250L))
        .thenReturn(
            Result.ok(
                new BalanceAdjustmentService.BalanceAdjustmentResult(
                    TEST_GUILD_ID, TEST_USER_ID, 500L, 750L, 250L, "Coins", "🪙")));

    Result<ProductRewardService.RewardGrantResult, ltdjms.discord.shared.DomainError> result =
        productRewardService.grantReward(
            new ProductRewardService.RewardGrantRequest(
                TEST_GUILD_ID,
                TEST_USER_ID,
                product,
                250L,
                "商品獎勵: 禮包",
                CurrencyTransaction.Source.PRODUCT_REWARD,
                GameTokenTransaction.Source.PRODUCT_REWARD));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().amount()).isEqualTo(250L);
    assertThat(result.getValue().currencyBalanceAfter()).isEqualTo(750L);
    verify(currencyTransactionService)
        .recordTransaction(
            TEST_GUILD_ID,
            TEST_USER_ID,
            250L,
            750L,
            CurrencyTransaction.Source.PRODUCT_REWARD,
            "商品獎勵: 禮包");
    verifyNoInteractions(gameTokenService, gameTokenTransactionService);
  }

  @Test
  @DisplayName("should grant token reward and record transaction")
  void shouldGrantTokenRewardAndRecordTransaction() {
    Product product =
        new Product(
            1L,
            TEST_GUILD_ID,
            "代幣包",
            null,
            Product.RewardType.TOKEN,
            50L,
            null,
            Instant.now(),
            Instant.now());
    when(gameTokenService.tryAdjustTokens(TEST_GUILD_ID, TEST_USER_ID, 75L))
        .thenReturn(
            Result.ok(
                new GameTokenService.TokenAdjustmentResult(
                    TEST_GUILD_ID, TEST_USER_ID, 0L, 75L, 75L)));

    Result<ProductRewardService.RewardGrantResult, ltdjms.discord.shared.DomainError> result =
        productRewardService.grantReward(
            new ProductRewardService.RewardGrantRequest(
                TEST_GUILD_ID,
                TEST_USER_ID,
                product,
                75L,
                "商品獎勵: 代幣包",
                CurrencyTransaction.Source.PRODUCT_REWARD,
                GameTokenTransaction.Source.PRODUCT_REWARD));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().amount()).isEqualTo(75L);
    assertThat(result.getValue().tokenBalanceAfter()).isEqualTo(75L);
    verify(gameTokenTransactionService)
        .recordTransaction(
            TEST_GUILD_ID,
            TEST_USER_ID,
            75L,
            75L,
            GameTokenTransaction.Source.PRODUCT_REWARD,
            "商品獎勵: 代幣包");
    verifyNoInteractions(balanceAdjustmentService, currencyTransactionService);
  }
}
