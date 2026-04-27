package ltdjms.discord.shop.domain;

import java.time.Instant;
import java.util.Objects;

/** Fiat order tracked until payment callback marks it as paid or expired. */
public record FiatOrder(
    Long id,
    long guildId,
    long buyerUserId,
    long productId,
    String productName,
    String orderNumber,
    String paymentNo,
    long amountTwd,
    Status status,
    String tradeStatus,
    String paymentMessage,
    Instant paidAt,
    Instant expireAt,
    Instant expiredAt,
    String terminalReason,
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
    PAID,
    EXPIRED
  }

  public FiatOrder {
    Objects.requireNonNull(productName, "productName must not be null");
    Objects.requireNonNull(orderNumber, "orderNumber must not be null");
    Objects.requireNonNull(paymentNo, "paymentNo must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(expireAt, "expireAt must not be null");
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
    if (orderNumber.length() > 32) {
      throw new IllegalArgumentException("orderNumber must not exceed 32 characters");
    }
    if (paymentNo.length() > 32) {
      throw new IllegalArgumentException("paymentNo must not exceed 32 characters");
    }
    if (amountTwd <= 0) {
      throw new IllegalArgumentException("amountTwd must be positive");
    }
    if (status == Status.PENDING_PAYMENT && paidAt != null) {
      throw new IllegalArgumentException("paidAt must be null when status is PENDING_PAYMENT");
    }
    if (status == Status.PAID && paidAt == null) {
      throw new IllegalArgumentException("paidAt is required when status is PAID");
    }
    if (status == Status.EXPIRED) {
      if (paidAt != null) {
        throw new IllegalArgumentException("paidAt must be null when status is EXPIRED");
      }
      if (expiredAt == null) {
        throw new IllegalArgumentException("expiredAt is required when status is EXPIRED");
      }
      if (terminalReason == null || terminalReason.isBlank()) {
        throw new IllegalArgumentException("terminalReason is required when status is EXPIRED");
      }
    } else if (expiredAt != null) {
      throw new IllegalArgumentException("expiredAt must be null unless status is EXPIRED");
    }
    if (terminalReason != null && terminalReason.isBlank()) {
      throw new IllegalArgumentException("terminalReason must not be blank when provided");
    }
  }

  public static FiatOrder createPending(
      long guildId,
      long buyerUserId,
      long productId,
      String productName,
      String orderNumber,
      String paymentNo,
      long amountTwd,
      Instant expireAt) {
    Instant now = Instant.now();
    return new FiatOrder(
        null,
        guildId,
        buyerUserId,
        productId,
        productName,
        orderNumber,
        paymentNo,
        amountTwd,
        Status.PENDING_PAYMENT,
        null,
        null,
        null,
        expireAt,
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

  public boolean isPaid() {
    return status == Status.PAID;
  }

  public boolean isExpired() {
    return status == Status.EXPIRED;
  }

  public boolean isTerminal() {
    return status == Status.PAID || status == Status.EXPIRED;
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
