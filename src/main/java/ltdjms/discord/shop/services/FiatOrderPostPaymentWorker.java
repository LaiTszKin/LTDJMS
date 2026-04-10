package ltdjms.discord.shop.services;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.currency.domain.CurrencyTransaction;
import ltdjms.discord.gametoken.domain.GameTokenTransaction;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.services.ProductRewardService;
import ltdjms.discord.product.services.ProductService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shop.domain.FiatOrder;
import ltdjms.discord.shop.domain.FiatOrderRepository;

/** Processes paid fiat orders asynchronously and idempotently. */
public class FiatOrderPostPaymentWorker {

  private static final Logger LOG = LoggerFactory.getLogger(FiatOrderPostPaymentWorker.class);
  private static final int DEFAULT_BATCH_SIZE = 20;

  private final FiatOrderRepository fiatOrderRepository;
  private final ProductService productService;
  private final ProductRewardService productRewardService;
  private final ShopAdminNotificationService adminNotificationService;
  private final FiatOrderBuyerNotificationService buyerNotificationService;
  private final Clock clock;

  public FiatOrderPostPaymentWorker(
      FiatOrderRepository fiatOrderRepository,
      ProductService productService,
      ProductRewardService productRewardService,
      ShopAdminNotificationService adminNotificationService,
      FiatOrderBuyerNotificationService buyerNotificationService) {
    this(
        fiatOrderRepository,
        productService,
        productRewardService,
        adminNotificationService,
        buyerNotificationService,
        Clock.systemUTC());
  }

  FiatOrderPostPaymentWorker(
      FiatOrderRepository fiatOrderRepository,
      ProductService productService,
      ProductRewardService productRewardService,
      ShopAdminNotificationService adminNotificationService,
      FiatOrderBuyerNotificationService buyerNotificationService,
      Clock clock) {
    this.fiatOrderRepository = Objects.requireNonNull(fiatOrderRepository);
    this.productService = Objects.requireNonNull(productService);
    this.productRewardService = Objects.requireNonNull(productRewardService);
    this.adminNotificationService = Objects.requireNonNull(adminNotificationService);
    this.buyerNotificationService = Objects.requireNonNull(buyerNotificationService);
    this.clock = Objects.requireNonNull(clock);
  }

  public void processPendingOrders() {
    List<FiatOrder> orders = fiatOrderRepository.findOrdersPendingPostPayment(DEFAULT_BATCH_SIZE);
    for (FiatOrder order : orders) {
      processSingleOrder(order);
    }
  }

  void processSingleOrder(FiatOrder order) {
    Instant claimTime = Instant.now(clock);
    if (!fiatOrderRepository.claimFulfillmentProcessing(order.orderNumber(), claimTime)) {
      return;
    }

    try {
      Product product = productService.getProduct(order.productId()).orElse(null);
      if (product == null) {
        throw new IllegalStateException("找不到已付款訂單對應商品");
      }

      if (!order.isBuyerNotified()) {
        buyerNotificationService.notifyPaymentSucceeded(order);
        fiatOrderRepository.markBuyerNotifiedIfNeeded(order.orderNumber(), Instant.now(clock));
      }

      if (product.shouldAutoCreateEscortOrder() && !order.isAdminNotified()) {
        Instant adminClaimTime = Instant.now(clock);
        if (fiatOrderRepository.claimAdminNotificationProcessing(
            order.orderNumber(), adminClaimTime)) {
          try {
            adminNotificationService.notifyAdminsOrderCreated(
                order.guildId(), order.buyerUserId(), product, "法幣付款完成", order.orderNumber());
            fiatOrderRepository.markAdminNotifiedIfNeeded(order.orderNumber(), adminClaimTime);
          } catch (Exception e) {
            fiatOrderRepository.releaseAdminNotificationProcessing(order.orderNumber());
            throw e;
          }
        }
      }

      if (product.hasReward() && !order.isRewardGranted()) {
        Result<ProductRewardService.RewardGrantResult, DomainError> rewardResult =
            productRewardService.grantReward(
                new ProductRewardService.RewardGrantRequest(
                    order.guildId(),
                    order.buyerUserId(),
                    product,
                    product.rewardAmount(),
                    String.format("法幣商品獎勵: %s", product.name()),
                    CurrencyTransaction.Source.PRODUCT_REWARD,
                    GameTokenTransaction.Source.PRODUCT_REWARD));
        if (rewardResult.isErr()) {
          throw new IllegalStateException(rewardResult.getError().message());
        }
        fiatOrderRepository.markRewardGrantedIfNeeded(order.orderNumber(), Instant.now(clock));
      }

      fiatOrderRepository.markFulfilledIfNeeded(order.orderNumber(), Instant.now(clock));
    } catch (Exception e) {
      fiatOrderRepository.releaseFulfillmentProcessing(order.orderNumber());
      LOG.warn("Failed to process paid fiat order: orderNumber={}", order.orderNumber(), e);
    }
  }
}
