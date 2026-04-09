package ltdjms.discord.shop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.shared.di.JDAProvider;
import ltdjms.discord.shop.domain.FiatOrder;

/** Sends buyer-facing direct-message notifications for fiat orders. */
public class FiatOrderBuyerNotificationService {

  private static final Logger LOG =
      LoggerFactory.getLogger(FiatOrderBuyerNotificationService.class);

  public void notifyPaymentSucceeded(FiatOrder order) {
    if (order == null) {
      return;
    }

    try {
      JDAProvider.getJda()
          .retrieveUserById(order.buyerUserId())
          .queue(
              buyerUser ->
                  buyerUser
                      .openPrivateChannel()
                      .queue(
                          channel ->
                              channel
                                  .sendMessage(buildPaymentSucceededMessage(order))
                                  .queue(
                                      success -> {
                                        // no-op
                                      },
                                      failure ->
                                          LOG.warn(
                                              "Failed to DM buyer paid notification:"
                                                  + " orderNumber={}, buyerUserId={}",
                                              order.orderNumber(),
                                              order.buyerUserId(),
                                              failure)),
                          failure ->
                              LOG.warn(
                                  "Failed to open buyer DM for paid notification:"
                                      + " orderNumber={}, buyerUserId={}",
                                  order.orderNumber(),
                                  order.buyerUserId(),
                                  failure)),
              failure ->
                  LOG.warn(
                      "Failed to retrieve buyer user for paid notification: orderNumber={},"
                          + " buyerUserId={}",
                      order.orderNumber(),
                      order.buyerUserId(),
                      failure));
    } catch (Exception e) {
      LOG.warn(
          "Unexpected error while notifying buyer payment success: orderNumber={},"
              + " buyerUserId={}",
          order.orderNumber(),
          order.buyerUserId(),
          e);
    }
  }

  String buildPaymentSucceededMessage(FiatOrder order) {
    StringBuilder builder = new StringBuilder();
    builder.append("✅ 付款成功！\n\n");
    builder.append("**商品：** ").append(order.productName()).append("\n");
    builder.append("**訂單編號：** `").append(order.orderNumber()).append("`\n");
    builder.append("**超商代碼：** `").append(order.paymentNo()).append("`\n");
    builder.append("**金額：** NT$ ").append(order.amountTwd()).append("\n\n");
    builder.append("我們已收到你的付款，後續若需查詢訂單進度，請提供訂單編號給管理員。");
    return builder.toString();
  }
}
