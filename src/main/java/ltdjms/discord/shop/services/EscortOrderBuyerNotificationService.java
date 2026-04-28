package ltdjms.discord.shop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.shared.runtime.DiscordRuntimeGateway;
import ltdjms.discord.shared.runtime.JdaDiscordRuntimeGateway;

/**
 * Sends buyer-facing direct-message notifications when an escort dispatch order is auto-created
 * after a product purchase. Notifies the buyer that the order is pending processing.
 */
public class EscortOrderBuyerNotificationService {

  private static final Logger LOG =
      LoggerFactory.getLogger(EscortOrderBuyerNotificationService.class);
  private final DiscordRuntimeGateway discordRuntimeGateway;

  public EscortOrderBuyerNotificationService() {
    this(new JdaDiscordRuntimeGateway());
  }

  public EscortOrderBuyerNotificationService(DiscordRuntimeGateway discordRuntimeGateway) {
    this.discordRuntimeGateway = discordRuntimeGateway;
  }

  /**
   * Sends a DM to the buyer notifying them that their escort order has been created and is waiting
   * for processing. Best-effort delivery; failures are logged as warnings and do not propagate.
   */
  public void notifyEscortOrderCreated(EscortDispatchOrder order) {
    if (order == null) {
      return;
    }

    // Skip notification if the buyer is the bot itself (bots cannot DM themselves)
    Long selfUserId = resolveSelfUserId();
    if (selfUserId != null && order.customerUserId() == selfUserId.longValue()) {
      LOG.debug("Skipping buyer escort notification for bot self: userId={}", selfUserId);
      return;
    }

    try {
      discordRuntimeGateway
          .retrieveUserById(order.customerUserId())
          .queue(
              buyerUser ->
                  buyerUser
                      .openPrivateChannel()
                      .queue(
                          channel ->
                              channel
                                  .sendMessage(buildEscortOrderCreatedMessage(order))
                                  .queue(
                                      success -> {
                                        // no-op
                                      },
                                      failure ->
                                          LOG.warn(
                                              "Failed to DM buyer escort order created:"
                                                  + " orderNumber={}, buyerUserId={}",
                                              order.orderNumber(),
                                              order.customerUserId(),
                                              failure)),
                          failure ->
                              LOG.warn(
                                  "Failed to open buyer DM for escort order notification:"
                                      + " orderNumber={}, buyerUserId={}",
                                  order.orderNumber(),
                                  order.customerUserId(),
                                  failure)),
              failure ->
                  LOG.warn(
                      "Failed to retrieve buyer user for escort order notification: orderNumber={},"
                          + " buyerUserId={}",
                      order.orderNumber(),
                      order.customerUserId(),
                      failure));
    } catch (Exception e) {
      LOG.warn(
          "Unexpected error while notifying buyer escort order created: orderNumber={},"
              + " buyerUserId={}",
          order.orderNumber(),
          order.customerUserId(),
          e);
    }
  }

  String buildEscortOrderCreatedMessage(EscortDispatchOrder order) {
    StringBuilder builder = new StringBuilder();
    builder.append("🛡️ 護航訂單已建立，正在等待處理\n\n");
    if (order.sourceProductName() != null && !order.sourceProductName().isBlank()) {
      builder.append("**商品：** ").append(order.sourceProductName()).append("\n");
    }
    builder.append("**護航訂單編號：** `").append(order.orderNumber()).append("`\n");
    builder.append("**付款方式：** ").append(formatPaymentMethod(order)).append("\n");
    builder.append("\n我們已收到你的訂單，管理員將會在不久後為你安排護航，請耐心等候。");
    return builder.toString();
  }

  private Long resolveSelfUserId() {
    try {
      return discordRuntimeGateway.getSelfUserId();
    } catch (RuntimeException e) {
      LOG.debug("Unable to resolve bot self user id for escort order notification", e);
      return null;
    }
  }

  private String formatPaymentMethod(EscortDispatchOrder order) {
    if (order.sourceType() == null) {
      return "未知";
    }
    return switch (order.sourceType()) {
      case CURRENCY_PURCHASE -> {
        if (order.sourceCurrencyPrice() != null && order.sourceCurrencyPrice() > 0) {
          yield String.format("貨幣（%,d 貨幣）", order.sourceCurrencyPrice());
        }
        yield "貨幣";
      }
      case FIAT_PAYMENT -> {
        if (order.sourceFiatPriceTwd() != null && order.sourceFiatPriceTwd() > 0) {
          yield String.format("法幣（NT$%,d）", order.sourceFiatPriceTwd());
        }
        yield "法幣";
      }
      case MANUAL -> "手動派單";
    };
  }
}
