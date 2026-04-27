package ltdjms.discord.shop.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.services.EscortDispatchHandoffService;
import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.services.ProductRewardService;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shop.domain.FiatOrder;
import ltdjms.discord.shop.domain.FiatOrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FiatOrderPostPaymentWorker 測試")
class FiatOrderPostPaymentWorkerTest {

  private static final Instant NOW = Instant.parse("2026-04-11T10:00:00Z");

  @Mock private FiatOrderRepository fiatOrderRepository;
  @Mock private ltdjms.discord.product.services.ProductService productService;
  @Mock private ProductRewardService productRewardService;
  @Mock private EscortDispatchHandoffService escortDispatchHandoffService;
  @Mock private ShopAdminNotificationService adminNotificationService;
  @Mock private FiatOrderBuyerNotificationService buyerNotificationService;

  private FiatOrderPostPaymentWorker worker;

  @BeforeEach
  void setUp() {
    worker =
        new FiatOrderPostPaymentWorker(
            fiatOrderRepository,
            productService,
            productRewardService,
            escortDispatchHandoffService,
            adminNotificationService,
            buyerNotificationService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("應完成已付款訂單的通知、獎勵與 fulfilled 標記")
  void shouldProcessPaidOrderSuccessfully() {
    FiatOrder order = paidOrder();
    Product product = rewardedEscortProduct();
    when(fiatOrderRepository.claimFulfillmentProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(productService.getProduct(order.productId())).thenReturn(Optional.of(product));
    EscortDispatchOrder dispatchOrder = autoDispatchOrder(order, product);
    when(escortDispatchHandoffService.handoffFromFiatPayment(
            eq(order.guildId()), eq(order.buyerUserId()), eq(product), eq(order.orderNumber())))
        .thenReturn(Result.ok(dispatchOrder));
    when(fiatOrderRepository.claimAdminNotificationProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(productRewardService.grantReward(any()))
        .thenReturn(Result.ok(new ProductRewardService.RewardGrantResult(50L, 150L, null)));

    worker.processSingleOrder(order);

    var callOrder =
        inOrder(buyerNotificationService, escortDispatchHandoffService, adminNotificationService);
    callOrder.verify(buyerNotificationService).notifyPaymentSucceeded(order);
    callOrder
        .verify(escortDispatchHandoffService)
        .handoffFromFiatPayment(order.guildId(), order.buyerUserId(), product, order.orderNumber());
    callOrder
        .verify(adminNotificationService)
        .notifyAdminsOrderCreated(eq(order.guildId()), eq(order.buyerUserId()), eq(dispatchOrder));
    verify(fiatOrderRepository).markBuyerNotifiedIfNeeded(eq(order.orderNumber()), any());
    verify(fiatOrderRepository).markAdminNotifiedIfNeeded(eq(order.orderNumber()), any());
    verify(productRewardService)
        .grantReward(
            eq(
                new ProductRewardService.RewardGrantRequest(
                    order.guildId(),
                    order.buyerUserId(),
                    product,
                    product.rewardAmount(),
                    "法幣商品獎勵: " + product.name(),
                    CurrencyTransaction.Source.PRODUCT_REWARD,
                    GameTokenTransaction.Source.PRODUCT_REWARD)));
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

    verify(productService, never()).getProduct(anyLong());
    verify(buyerNotificationService, never()).notifyPaymentSucceeded(any());
    verify(fiatOrderRepository, never()).markFulfilledIfNeeded(any(), any());
  }

  @Test
  @DisplayName("管理員通知失敗時應釋放 claim 並保留 fulfilled 未完成")
  void shouldReleaseClaimsWhenAdminNotificationFails() {
    FiatOrder order = paidOrder();
    Product product = escortOnlyProduct();
    when(fiatOrderRepository.claimFulfillmentProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    when(productService.getProduct(order.productId())).thenReturn(Optional.of(product));
    when(escortDispatchHandoffService.handoffFromFiatPayment(
            eq(order.guildId()), eq(order.buyerUserId()), eq(product), eq(order.orderNumber())))
        .thenReturn(Result.ok(autoDispatchOrder(order, product)));
    when(fiatOrderRepository.claimAdminNotificationProcessing(eq(order.orderNumber()), any()))
        .thenReturn(true);
    org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
        .when(adminNotificationService)
        .notifyAdminsOrderCreated(anyLong(), anyLong(), any(EscortDispatchOrder.class));

    worker.processSingleOrder(order);

    verify(fiatOrderRepository).releaseAdminNotificationProcessing(order.orderNumber());
    verify(fiatOrderRepository).releaseFulfillmentProcessing(order.orderNumber());
    verify(fiatOrderRepository, never()).markFulfilledIfNeeded(any(), any());
    verify(escortDispatchHandoffService)
        .handoffFromFiatPayment(order.guildId(), order.buyerUserId(), product, order.orderNumber());
  }

  private FiatOrder paidOrder() {
    return new FiatOrder(
        1L,
        123L,
        456L,
        789L,
        "護航商品",
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

  private Product rewardedEscortProduct() {
    return new Product(
        789L,
        123L,
        "護航商品",
        "desc",
        Product.RewardType.CURRENCY,
        50L,
        null,
        1200L,
        true,
        "escort-a",
        NOW,
        NOW);
  }

  private Product escortOnlyProduct() {
    return new Product(
        789L, 123L, "護航商品", "desc", null, null, null, 1200L, true, "escort-a", NOW, NOW);
  }

  private EscortDispatchOrder autoDispatchOrder(FiatOrder order, Product product) {
    return EscortDispatchOrder.createAutoHandoff(
        "ESC-20260411-ABC123",
        order.guildId(),
        0L,
        0L,
        order.buyerUserId(),
        EscortDispatchOrder.SourceType.FIAT_PAYMENT,
        order.orderNumber(),
        product.id(),
        product.name(),
        product.currencyPrice(),
        product.fiatPriceTwd(),
        product.escortOptionCode());
  }
}
