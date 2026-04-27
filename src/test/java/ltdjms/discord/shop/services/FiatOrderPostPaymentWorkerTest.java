package ltdjms.discord.shop.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.product.services.ProductRewardService;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shop.domain.FiatOrder;
import ltdjms.discord.shop.domain.FiatOrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiatOrderPostPaymentWorker 測試")
class FiatOrderPostPaymentWorkerTest {

  private static final Instant NOW = Instant.parse("2026-04-11T10:00:00Z");

  @Mock private FiatOrderRepository fiatOrderRepository;
  @Mock private ProductRewardService productRewardService;
  @Mock private ShopAdminNotificationService adminNotificationService;
  @Mock private FiatOrderBuyerNotificationService buyerNotificationService;

  private FiatOrderPostPaymentWorker worker;

  @BeforeEach
  void setUp() {
    worker =
        new FiatOrderPostPaymentWorker(
            fiatOrderRepository,
            productRewardService,
            adminNotificationService,
            buyerNotificationService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("應完成已付款訂單的通知、獎勵與 fulfilled 標記")
  void shouldProcessPaidOrderSuccessfully() {
    FiatOrder order = paidOrder();
    when(fiatOrderRepository.claimFulfillmentProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(fiatOrderRepository.claimAdminNotificationProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(productRewardService.grantReward(any()))
        .thenReturn(Result.ok(new ProductRewardService.RewardGrantResult(50L, 150L, null)));

    worker.processSingleOrder(order);

    verify(buyerNotificationService).notifyPaymentSucceeded(order);
    verify(fiatOrderRepository).markBuyerNotifiedIfNeeded(eq(order.orderNumber()), any());
    verify(adminNotificationService)
        .notifyAdminsOrderCreated(
            eq(order.guildId()),
            eq(order.buyerUserId()),
            eq(order.toFulfillmentProduct()),
            eq("法幣付款完成"),
            eq(order.orderNumber()));
    verify(fiatOrderRepository).markAdminNotifiedIfNeeded(eq(order.orderNumber()), any());
    verify(productRewardService)
        .grantReward(
            eq(
                new ProductRewardService.RewardGrantRequest(
                    order.guildId(),
                    order.buyerUserId(),
                    order.toFulfillmentProduct(),
                    order.toFulfillmentProduct().rewardAmount(),
                    "法幣商品獎勵: " + order.toFulfillmentProduct().name(),
                    ltdjms.discord.currency.domain.CurrencyTransaction.Source.PRODUCT_REWARD,
                    ltdjms.discord.gametoken.domain.GameTokenTransaction.Source.PRODUCT_REWARD)));
    verify(fiatOrderRepository).markRewardGrantedIfNeeded(eq(order.orderNumber()), any());
    verify(fiatOrderRepository).markFulfilledIfNeeded(eq(order.orderNumber()), any());
    verify(fiatOrderRepository, never()).releaseFulfillmentProcessing(order.orderNumber());
  }

  @Test
  @DisplayName("無法取得處理 claim 時不應執行任何副作用")
  void shouldSkipWhenFulfillmentClaimFails() {
    FiatOrder order = paidOrder();
    when(fiatOrderRepository.claimFulfillmentProcessing(eq(order.orderNumber()), any()))
        .thenReturn(false);

    worker.processSingleOrder(order);

    verify(buyerNotificationService, never()).notifyPaymentSucceeded(any());
    verify(fiatOrderRepository, never()).markFulfilledIfNeeded(any(), any());
  }

  @Test
  @DisplayName("管理員通知失敗時應釋放 claim 並保留 fulfilled 未完成")
  void shouldReleaseClaimsWhenAdminNotificationFails() {
    FiatOrder order = paidOrder();
    when(fiatOrderRepository.claimFulfillmentProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(fiatOrderRepository.claimAdminNotificationProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
        .when(adminNotificationService)
        .notifyAdminsOrderCreated(
            anyLong(), anyLong(), eq(order.toFulfillmentProduct()), any(), any());

    worker.processSingleOrder(order);

    verify(fiatOrderRepository).releaseAdminNotificationProcessing(order.orderNumber());
    verify(fiatOrderRepository).releaseFulfillmentProcessing(order.orderNumber());
    verify(fiatOrderRepository, never()).markFulfilledIfNeeded(any(), any());
  }

  private FiatOrder paidOrder() {
    return new FiatOrder(
        1L,
        123L,
        456L,
        789L,
        "護航商品",
        ltdjms.discord.product.domain.Product.RewardType.CURRENCY,
        50L,
        true,
        "ESCORT-A",
        "FD260411000001",
        "CVS123456",
        1200L,
        FiatOrder.Status.PAID,
        "1",
        "付款成功",
        NOW,
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        NOW,
        NOW);
  }
}
