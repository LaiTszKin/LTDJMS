package ltdjms.discord.dispatch.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 派單系統的護航訂單實體。
 *
 * <p>狀態流轉：
 *
 * <pre>
 * PENDING_CONFIRMATION -> CONFIRMED -> PENDING_CUSTOMER_CONFIRMATION -> COMPLETED
 *                                           └-> AFTER_SALES_REQUESTED -> AFTER_SALES_IN_PROGRESS
 *                                                                         -> AFTER_SALES_CLOSED
 * </pre>
 */
public record EscortDispatchOrder(
    Long id,
    String orderNumber,
    long guildId,
    long assignedByUserId,
    long escortUserId,
    long customerUserId,
    Instant createdAt,
    Instant confirmedAt,
    Instant completionRequestedAt,
    Instant completedAt,
    Instant afterSalesRequestedAt,
    Long afterSalesAssigneeUserId,
    Instant afterSalesAssignedAt,
    Instant afterSalesClosedAt,
    Instant updatedAt,
    SourceType sourceType,
    String sourceReference,
    Long sourceProductId,
    String sourceProductName,
    Long sourceCurrencyPrice,
    Long sourceFiatPriceTwd,
    String sourceEscortOptionCode,
    Status status) {

  public static final Duration CUSTOMER_CONFIRM_TIMEOUT = Duration.ofHours(24);

  /** 訂單狀態。 */
  public enum Status {
    /** 已建立，等待護航者確認。 */
    PENDING_CONFIRMATION,
    /** 護航者已確認接單。 */
    CONFIRMED,
    /** 護航者已送出完單，等待客戶確認或申請售後。 */
    PENDING_CUSTOMER_CONFIRMATION,
    /** 訂單已完成（客戶確認或超時自動完成）。 */
    COMPLETED,
    /** 客戶已提出售後申請。 */
    AFTER_SALES_REQUESTED,
    /** 售後人員已接手處理。 */
    AFTER_SALES_IN_PROGRESS,
    /** 售後案件已結案。 */
    AFTER_SALES_CLOSED
  }

  /** 護航訂單來源類型。 */
  public enum SourceType {
    MANUAL,
    CURRENCY_PURCHASE,
    FIAT_PAYMENT
  }

  public EscortDispatchOrder {
    Objects.requireNonNull(orderNumber, "orderNumber must not be null");
    Objects.requireNonNull(sourceType, "sourceType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    if (orderNumber.isBlank()) {
      throw new IllegalArgumentException("orderNumber must not be blank");
    }
    if (orderNumber.length() > 32) {
      throw new IllegalArgumentException("orderNumber must not exceed 32 characters");
    }
    if (sourceType == SourceType.MANUAL) {
      if (sourceReference != null
          || sourceProductId != null
          || sourceProductName != null
          || sourceCurrencyPrice != null
          || sourceFiatPriceTwd != null
          || sourceEscortOptionCode != null) {
        throw new IllegalArgumentException("manual dispatch order must not carry source snapshot");
      }
    } else {
      if (sourceReference == null || sourceReference.isBlank()) {
        throw new IllegalArgumentException("sourceReference must not be blank");
      }
      if (sourceProductId == null) {
        throw new IllegalArgumentException("sourceProductId must not be null");
      }
      if (sourceProductName == null || sourceProductName.isBlank()) {
        throw new IllegalArgumentException("sourceProductName must not be blank");
      }
      if (sourceEscortOptionCode == null || sourceEscortOptionCode.isBlank()) {
        throw new IllegalArgumentException("sourceEscortOptionCode must not be blank");
      }
      if (sourceCurrencyPrice == null && sourceFiatPriceTwd == null) {
        throw new IllegalArgumentException("source price snapshot must not be empty");
      }
    }
    if (escortUserId == customerUserId) {
      throw new IllegalArgumentException("escortUserId and customerUserId must be different");
    }

    switch (status) {
      case PENDING_CONFIRMATION -> {
        // no-op
      }
      case CONFIRMED -> requireNonNull("confirmedAt", confirmedAt);
      case PENDING_CUSTOMER_CONFIRMATION -> {
        requireNonNull("confirmedAt", confirmedAt);
        requireNonNull("completionRequestedAt", completionRequestedAt);
      }
      case COMPLETED -> {
        requireNonNull("completedAt", completedAt);
      }
      case AFTER_SALES_REQUESTED -> {
        requireNonNull("afterSalesRequestedAt", afterSalesRequestedAt);
      }
      case AFTER_SALES_IN_PROGRESS -> {
        requireNonNull("afterSalesRequestedAt", afterSalesRequestedAt);
        requireNonNull("afterSalesAssigneeUserId", afterSalesAssigneeUserId);
        requireNonNull("afterSalesAssignedAt", afterSalesAssignedAt);
      }
      case AFTER_SALES_CLOSED -> {
        requireNonNull("afterSalesAssigneeUserId", afterSalesAssigneeUserId);
        requireNonNull("afterSalesClosedAt", afterSalesClosedAt);
      }
    }
  }

  /** 建立待確認的新訂單（尚未持久化，id 為 null）。 */
  public static EscortDispatchOrder createPending(
      String orderNumber,
      long guildId,
      long assignedByUserId,
      long escortUserId,
      long customerUserId) {
    return createPending(
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        SourceType.MANUAL,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static EscortDispatchOrder createPending(
      String orderNumber,
      long guildId,
      long assignedByUserId,
      long escortUserId,
      long customerUserId,
      SourceType sourceType,
      String sourceReference,
      Long sourceProductId,
      String sourceProductName,
      Long sourceCurrencyPrice,
      Long sourceFiatPriceTwd,
      String sourceEscortOptionCode) {
    Instant now = Instant.now();
    return new EscortDispatchOrder(
        null,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        now,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        now,
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.PENDING_CONFIRMATION);
  }

  public static EscortDispatchOrder createAutoHandoff(
      String orderNumber,
      long guildId,
      long assignedByUserId,
      long escortUserId,
      long customerUserId,
      SourceType sourceType,
      String sourceReference,
      Long sourceProductId,
      String sourceProductName,
      Long sourceCurrencyPrice,
      Long sourceFiatPriceTwd,
      String sourceEscortOptionCode) {
    Instant now = Instant.now();
    return new EscortDispatchOrder(
        null,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        now,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        now,
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.PENDING_CONFIRMATION);
  }

  /** 由指定護航者確認後回傳新狀態實體。 */
  public EscortDispatchOrder withConfirmed(Instant confirmedAt) {
    Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        null,
        null,
        null,
        null,
        null,
        null,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.CONFIRMED);
  }

  /** 護航者送出完單，等待客戶確認。 */
  public EscortDispatchOrder withCompletionRequested(Instant requestedAt) {
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        requestedAt,
        null,
        null,
        null,
        null,
        null,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.PENDING_CUSTOMER_CONFIRMATION);
  }

  public EscortDispatchOrder withCompleted(Instant completedAt) {
    Objects.requireNonNull(completedAt, "completedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        completionRequestedAt,
        completedAt,
        afterSalesRequestedAt,
        afterSalesAssigneeUserId,
        afterSalesAssignedAt,
        afterSalesClosedAt,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.COMPLETED);
  }

  public EscortDispatchOrder withAfterSalesRequested(Instant requestedAt) {
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        completionRequestedAt,
        completedAt,
        requestedAt,
        null,
        null,
        null,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.AFTER_SALES_REQUESTED);
  }

  public EscortDispatchOrder withAfterSalesInProgress(long assigneeUserId, Instant assignedAt) {
    Objects.requireNonNull(assignedAt, "assignedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        completionRequestedAt,
        completedAt,
        afterSalesRequestedAt,
        assigneeUserId,
        assignedAt,
        null,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.AFTER_SALES_IN_PROGRESS);
  }

  public EscortDispatchOrder withAfterSalesClosed(Instant closedAt) {
    Objects.requireNonNull(closedAt, "closedAt must not be null");
    return new EscortDispatchOrder(
        id,
        orderNumber,
        guildId,
        assignedByUserId,
        escortUserId,
        customerUserId,
        createdAt,
        confirmedAt,
        completionRequestedAt,
        completedAt,
        afterSalesRequestedAt,
        afterSalesAssigneeUserId,
        afterSalesAssignedAt,
        closedAt,
        Instant.now(),
        sourceType,
        sourceReference,
        sourceProductId,
        sourceProductName,
        sourceCurrencyPrice,
        sourceFiatPriceTwd,
        sourceEscortOptionCode,
        Status.AFTER_SALES_CLOSED);
  }

  public boolean isPendingEscortConfirmation() {
    return status == Status.PENDING_CONFIRMATION;
  }

  public boolean isConfirmed() {
    return status == Status.CONFIRMED;
  }

  public boolean isPendingCustomerConfirmation() {
    return status == Status.PENDING_CUSTOMER_CONFIRMATION;
  }

  public boolean isAfterSalesRequested() {
    return status == Status.AFTER_SALES_REQUESTED;
  }

  public boolean isAfterSalesInProgress() {
    return status == Status.AFTER_SALES_IN_PROGRESS;
  }

  public boolean isCompleted() {
    return status == Status.COMPLETED || status == Status.AFTER_SALES_CLOSED;
  }

  public boolean canBeConfirmedBy(long userId) {
    return escortUserId == userId;
  }

  public boolean canBeCompletedByEscort(long userId) {
    return escortUserId == userId;
  }

  public boolean canBeConfirmedByCustomer(long userId) {
    return customerUserId == userId;
  }

  public boolean isAfterSalesAssignee(long userId) {
    return afterSalesAssigneeUserId != null && afterSalesAssigneeUserId == userId;
  }

  public boolean isManualSource() {
    return sourceType == SourceType.MANUAL;
  }

  public boolean isAutoSource() {
    return sourceType != SourceType.MANUAL;
  }

  public boolean hasCustomerConfirmationTimedOut(Instant now) {
    if (!isPendingCustomerConfirmation() || completionRequestedAt == null) {
      return false;
    }
    Instant deadline = completionRequestedAt.plus(CUSTOMER_CONFIRM_TIMEOUT);
    return !deadline.isAfter(now);
  }

  private static void requireNonNull(String field, Object value) {
    if (value == null) {
      throw new IllegalArgumentException(field + " must not be null");
    }
  }
}
