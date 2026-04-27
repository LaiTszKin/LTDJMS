package ltdjms.discord.shop.domain;

import java.time.Instant;
import java.util.Objects;

import ltdjms.discord.product.domain.Product;

/** Fiat order tracked until payment callback marks it as paid. */
public record FiatOrder(
    Long id,
    long guildId,
    long buyerUserId,
    long productId,
    String productName,
    Product.RewardType fulfillmentRewardType,
    Long fulfillmentRewardAmount,
    boolean fulfillmentAutoCreateEscortOrder,
    String fulfillmentEscortOptionCode,
    String orderNumber,
    String paymentNo,
    long amountTwd,
    Status status,
    String tradeStatus,
    String paymentMessage,
    Instant paidAt,
    Instant buyerNotifiedAt,
    Instant rewardGrantedAt,
    Instant fulfilledAt,
    Instant adminNotifiedAt,
    String lastCallbackPayload,
    int reconciliationAttemptCount,
    Instant reconciliationNextAttemptAt,
    Instant createdAt,
    Instant updatedAt) {

  public enum Status {
    PENDING_PAYMENT,
    PAID
  }

  public FiatOrder {
    Objects.requireNonNull(productName, "productName must not be null");
    Objects.requireNonNull(orderNumber, "orderNumber must not be null");
    Objects.requireNonNull(paymentNo, "paymentNo must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    if (productName.isBlank()) {
      throw new IllegalArgumentException("productName must not be blank");
    }
    if (orderNumber.isBlank()) {
      throw new IllegalArgumentException("orderNumber must not be blank");
    }
    if (paymentNo.isBlank()) {
      throw new IllegalArgumentException("paymentNo must not be blank");
    }
    if (productName.length() > 100) {
      throw new IllegalArgumentException("productName must not exceed 100 characters");
    }
    if ((fulfillmentRewardType == null) != (fulfillmentRewardAmount == null)) {
      throw new IllegalArgumentException(
          "fulfillmentRewardType and fulfillmentRewardAmount must both be specified or both be"
              + " null");
    }
    if (fulfillmentRewardAmount != null && fulfillmentRewardAmount <= 0) {
      throw new IllegalArgumentException("fulfillmentRewardAmount must be positive");
    }
    if (fulfillmentEscortOptionCode != null && fulfillmentEscortOptionCode.length() > 120) {
      throw new IllegalArgumentException(
          "fulfillmentEscortOptionCode must not exceed 120 characters");
    }
    if (fulfillmentAutoCreateEscortOrder) {
      if (fulfillmentEscortOptionCode == null || fulfillmentEscortOptionCode.isBlank()) {
        throw new IllegalArgumentException(
            "fulfillmentEscortOptionCode is required when fulfillmentAutoCreateEscortOrder is"
                + " enabled");
      }
    } else if (fulfillmentEscortOptionCode != null && !fulfillmentEscortOptionCode.isBlank()) {
      throw new IllegalArgumentException(
          "fulfillmentEscortOptionCode requires fulfillmentAutoCreateEscortOrder to be enabled");
    }
    if (orderNumber.length() > 32) {
      throw new IllegalArgumentException("orderNumber must not exceed 32 characters");
    }
    if (paymentNo.length() > 32) {
      throw new IllegalArgumentException("paymentNo must not exceed 32 characters");
    }
    if (amountTwd <= 0) {
      throw new IllegalArgumentException("amountTwd must be positive");
    }
    if (status == Status.PAID && paidAt == null) {
      throw new IllegalArgumentException("paidAt is required when status is PAID");
    }
  }

  public static FiatOrder createPending(
      long guildId,
      long buyerUserId,
      long productId,
      String productName,
      Product.RewardType fulfillmentRewardType,
      Long fulfillmentRewardAmount,
      boolean fulfillmentAutoCreateEscortOrder,
      String fulfillmentEscortOptionCode,
      String orderNumber,
      String paymentNo,
      long amountTwd) {
    Instant now = Instant.now();
    return new FiatOrder(
        null,
        guildId,
        buyerUserId,
        productId,
        productName,
        fulfillmentRewardType,
        fulfillmentRewardAmount,
        fulfillmentAutoCreateEscortOrder,
        fulfillmentEscortOptionCode,
        orderNumber,
        paymentNo,
        amountTwd,
        Status.PENDING_PAYMENT,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        now,
        now);
  }

  public boolean hasFulfillmentReward() {
    return fulfillmentRewardType != null && fulfillmentRewardAmount != null;
  }

  public boolean shouldAutoCreateEscortOrder() {
    return fulfillmentAutoCreateEscortOrder
        && fulfillmentEscortOptionCode != null
        && !fulfillmentEscortOptionCode.isBlank();
  }

  public Product toFulfillmentProduct() {
    return new Product(
        productId,
        guildId,
        productName,
        null,
        fulfillmentRewardType,
        fulfillmentRewardAmount,
        null,
        amountTwd,
        fulfillmentAutoCreateEscortOrder,
        fulfillmentEscortOptionCode,
        createdAt,
        updatedAt);
  }

  public boolean isPaid() {
    return status == Status.PAID;
  }

  public boolean isFulfilled() {
    return fulfilledAt != null;
  }

  public boolean isBuyerNotified() {
    return buyerNotifiedAt != null;
  }

  public boolean isRewardGranted() {
    return rewardGrantedAt != null;
  }

  public boolean isAdminNotified() {
    return adminNotifiedAt != null;
  }
}
