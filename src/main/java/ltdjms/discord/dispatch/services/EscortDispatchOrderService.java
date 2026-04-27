package ltdjms.discord.dispatch.services;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.domain.EscortDispatchOrderRepository;
import ltdjms.discord.product.domain.EscortOrderOptionCatalog;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** 派單護航訂單核心服務。 */
public class EscortDispatchOrderService {

  private static final Logger LOG = LoggerFactory.getLogger(EscortDispatchOrderService.class);

  static final int MAX_ORDER_NUMBER_RETRIES = 20;
  static final int DEFAULT_HISTORY_LIMIT = 10;
  static final int DEFAULT_PENDING_ASSIGNMENT_LIMIT = 5;
  static final int MAX_HISTORY_LIMIT = 20;

  private final EscortDispatchOrderRepository repository;
  private final EscortDispatchOrderNumberGenerator orderNumberGenerator;
  private final Clock clock;

  public EscortDispatchOrderService(EscortDispatchOrderRepository repository) {
    this(repository, new EscortDispatchOrderNumberGenerator(), Clock.systemUTC());
  }

  public EscortDispatchOrderService(
      EscortDispatchOrderRepository repository,
      EscortDispatchOrderNumberGenerator orderNumberGenerator) {
    this(repository, orderNumberGenerator, Clock.systemUTC());
  }

  EscortDispatchOrderService(
      EscortDispatchOrderRepository repository,
      EscortDispatchOrderNumberGenerator orderNumberGenerator,
      Clock clock) {
    this.repository = repository;
    this.orderNumberGenerator = orderNumberGenerator;
    this.clock = clock;
  }

  /** 建立待確認的新派單訂單。 */
  public Result<EscortDispatchOrder, DomainError> createOrder(
      long guildId, long assignedByUserId, long escortUserId, long customerUserId) {

    if (escortUserId == customerUserId) {
      return Result.err(DomainError.invalidInput("護航者與客戶不能是同一人"));
    }

    try {
      String orderNumber = generateUniqueOrderNumber();
      EscortDispatchOrder order =
          EscortDispatchOrder.createPending(
              orderNumber, guildId, assignedByUserId, escortUserId, customerUserId);

      EscortDispatchOrder saved = repository.save(order);
      LOG.info(
          "Created escort dispatch order: orderNumber={}, guildId={}, escortUserId={},"
              + " customerUserId={}",
          saved.orderNumber(),
          guildId,
          escortUserId,
          customerUserId);
      return Result.ok(saved);
    } catch (Exception e) {
      LOG.error(
          "Failed to create escort dispatch order: guildId={}, escortUserId={}, customerUserId={}",
          guildId,
          escortUserId,
          customerUserId,
          e);
      return Result.err(DomainError.persistenceFailure("建立派單失敗", e));
    }
  }

  /** 手動建立尚未派發給護航者的護航訂單。 */
  public Result<EscortDispatchOrder, DomainError> createManualOpenOrder(
      long guildId, long assignedByUserId, long customerUserId, String escortOptionCode) {
    if (customerUserId <= 0) {
      return Result.err(DomainError.invalidInput("請選擇客戶"));
    }
    Result<String, DomainError> normalizedOptionResult = normalizeEscortOption(escortOptionCode);
    if (normalizedOptionResult.isErr()) {
      return Result.err(normalizedOptionResult.getError());
    }

    try {
      String orderNumber = generateUniqueOrderNumber();
      EscortDispatchOrder order =
          EscortDispatchOrder.createManualOpenOrder(
              orderNumber,
              guildId,
              assignedByUserId,
              customerUserId,
              normalizedOptionResult.getValue());

      EscortDispatchOrder saved = repository.save(order);
      LOG.info(
          "Created manual open escort dispatch order: orderNumber={}, guildId={},"
              + " customerUserId={}, escortOptionCode={}",
          saved.orderNumber(),
          guildId,
          customerUserId,
          normalizedOptionResult.getValue());
      return Result.ok(saved);
    } catch (Exception e) {
      LOG.error(
          "Failed to create manual open escort dispatch order: guildId={}, customerUserId={},"
              + " escortOptionCode={}",
          guildId,
          customerUserId,
          normalizedOptionResult.getValue(),
          e);
      return Result.err(DomainError.persistenceFailure("建立護航訂單失敗", e));
    }
  }

  /** 將既有待派發護航訂單派給指定護航者。 */
  public Result<EscortDispatchOrder, DomainError> assignPendingOrder(
      String orderNumber, long assignedByUserId, long escortUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.isPendingEscortConfirmation()) {
      return Result.err(DomainError.invalidInput("此訂單目前不可派發"));
    }
    if (order.escortUserId() != 0L) {
      return Result.err(DomainError.invalidInput("此訂單已派發給護航者"));
    }
    if (escortUserId <= 0) {
      return Result.err(DomainError.invalidInput("請選擇護航者"));
    }
    if (escortUserId == order.customerUserId()) {
      return Result.err(DomainError.invalidInput("護航者與客戶不能是同一人"));
    }

    try {
      Optional<EscortDispatchOrder> assigned =
          repository.assignEscort(order.orderNumber(), assignedByUserId, escortUserId, now());
      if (assigned.isPresent()) {
        LOG.info(
            "Assigned escort dispatch order: orderNumber={}, assignedByUserId={},"
                + " escortUserId={}",
            order.orderNumber(),
            assignedByUserId,
            escortUserId);
        return Result.ok(assigned.get());
      }
      return Result.err(DomainError.invalidInput("此訂單已被派發或目前不可派發"));
    } catch (Exception e) {
      LOG.error(
          "Failed to assign escort dispatch order: orderNumber={}, assignedByUserId={},"
              + " escortUserId={}",
          order.orderNumber(),
          assignedByUserId,
          escortUserId,
          e);
      return Result.err(DomainError.persistenceFailure("派發護航訂單失敗", e));
    }
  }

  /** 護航者確認接單。 */
  public Result<EscortDispatchOrder, DomainError> confirmOrder(
      String orderNumber, long confirmerUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.canBeConfirmedBy(confirmerUserId)) {
      return Result.err(DomainError.invalidInput("只有被指派的護航者可以確認此訂單"));
    }

    if (!order.isPendingEscortConfirmation()) {
      return Result.err(DomainError.invalidInput("此訂單已確認"));
    }

    try {
      EscortDispatchOrder confirmed = order.withConfirmed(now());
      EscortDispatchOrder updated = repository.update(confirmed);
      LOG.info(
          "Escort dispatch order confirmed: orderNumber={}, escortUserId={}",
          updated.orderNumber(),
          confirmerUserId);
      return Result.ok(updated);
    } catch (Exception e) {
      LOG.error(
          "Failed to confirm escort dispatch order: orderNumber={}, confirmerUserId={}",
          order.orderNumber(),
          confirmerUserId,
          e);
      return Result.err(DomainError.persistenceFailure("確認訂單失敗", e));
    }
  }

  /** 護航者完成服務，等待客戶確認。 */
  public Result<EscortDispatchOrder, DomainError> requestCompletion(
      String orderNumber, long escortUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.canBeCompletedByEscort(escortUserId)) {
      return Result.err(DomainError.invalidInput("只有被指派的護航者可以送出完成"));
    }

    if (!order.isConfirmed()) {
      return Result.err(DomainError.invalidInput("此訂單目前不可送出完成"));
    }

    try {
      EscortDispatchOrder updated = repository.update(order.withCompletionRequested(now()));
      LOG.info(
          "Escort dispatch order moved to pending customer confirmation: orderNumber={}",
          updated.orderNumber());
      return Result.ok(updated);
    } catch (Exception e) {
      LOG.error(
          "Failed to request completion for escort dispatch order: orderNumber={}, escortUserId={}",
          order.orderNumber(),
          escortUserId,
          e);
      return Result.err(DomainError.persistenceFailure("送出完成失敗", e));
    }
  }

  /** 客戶確認完成。 */
  public Result<EscortDispatchOrder, DomainError> customerConfirmCompletion(
      String orderNumber, long customerUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.canBeConfirmedByCustomer(customerUserId)) {
      return Result.err(DomainError.invalidInput("只有訂單客戶可以確認完成"));
    }

    if (order.isCompleted()) {
      return Result.ok(order);
    }

    if (!order.isPendingCustomerConfirmation()) {
      return Result.err(DomainError.invalidInput("此訂單目前不可由客戶確認完成"));
    }

    try {
      EscortDispatchOrder normalized = ensureTimeoutCompletion(order);
      if (normalized.isCompleted()) {
        return Result.ok(normalized);
      }

      EscortDispatchOrder updated = repository.update(normalized.withCompleted(now()));
      LOG.info(
          "Customer confirmed escort dispatch order completion: orderNumber={}",
          updated.orderNumber());
      return Result.ok(updated);
    } catch (Exception e) {
      LOG.error(
          "Failed to confirm escort dispatch order by customer: orderNumber={}, customerUserId={}",
          order.orderNumber(),
          customerUserId,
          e);
      return Result.err(DomainError.persistenceFailure("客戶確認完成失敗", e));
    }
  }

  /** 客戶提出售後申請。 */
  public Result<EscortDispatchOrder, DomainError> requestAfterSales(
      String orderNumber, long customerUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.canBeConfirmedByCustomer(customerUserId)) {
      return Result.err(DomainError.invalidInput("只有訂單客戶可以申請售後"));
    }

    try {
      EscortDispatchOrder normalized = ensureTimeoutCompletion(order);
      if (normalized.status() == EscortDispatchOrder.Status.AFTER_SALES_REQUESTED
          || normalized.status() == EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS
          || normalized.status() == EscortDispatchOrder.Status.AFTER_SALES_CLOSED) {
        return Result.err(DomainError.invalidInput("此訂單已在售後流程中"));
      }

      if (normalized.status() != EscortDispatchOrder.Status.PENDING_CUSTOMER_CONFIRMATION
          && normalized.status() != EscortDispatchOrder.Status.COMPLETED) {
        return Result.err(DomainError.invalidInput("此訂單目前不可申請售後"));
      }

      EscortDispatchOrder updated = repository.update(normalized.withAfterSalesRequested(now()));
      LOG.info(
          "Customer requested after-sales for escort dispatch order: orderNumber={}",
          updated.orderNumber());
      return Result.ok(updated);
    } catch (Exception e) {
      LOG.error(
          "Failed to request after-sales for escort dispatch order: orderNumber={},"
              + " customerUserId={}",
          order.orderNumber(),
          customerUserId,
          e);
      return Result.err(DomainError.persistenceFailure("申請售後失敗", e));
    }
  }

  /** 售後人員接手案件。 */
  public Result<EscortDispatchOrder, DomainError> claimAfterSales(
      String orderNumber, long afterSalesUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.isAfterSalesRequested()) {
      if (order.isAfterSalesInProgress()) {
        if (order.isAfterSalesAssignee(afterSalesUserId)) {
          return Result.err(DomainError.invalidInput("你已接手此售後案件"));
        }
        return Result.err(DomainError.invalidInput("此售後案件已由其他售後人員接手"));
      }
      if (order.status() == EscortDispatchOrder.Status.AFTER_SALES_CLOSED) {
        return Result.err(DomainError.invalidInput("此售後案件已結案"));
      }
      return Result.err(DomainError.invalidInput("此訂單目前不可接手售後"));
    }

    try {
      Optional<EscortDispatchOrder> claimed =
          repository.claimAfterSales(order.orderNumber(), afterSalesUserId, now());
      if (claimed.isPresent()) {
        return Result.ok(claimed.get());
      }

      Optional<EscortDispatchOrder> latest = repository.findByOrderNumber(order.orderNumber());
      if (latest.isPresent() && latest.get().isAfterSalesInProgress()) {
        return Result.err(DomainError.invalidInput("此售後案件已由其他售後人員接手"));
      }
      return Result.err(DomainError.invalidInput("此售後案件目前不可接手"));
    } catch (Exception e) {
      LOG.error(
          "Failed to claim after-sales case: orderNumber={}, afterSalesUserId={}",
          order.orderNumber(),
          afterSalesUserId,
          e);
      return Result.err(DomainError.persistenceFailure("接手售後案件失敗", e));
    }
  }

  /** 售後人員完成結案。 */
  public Result<EscortDispatchOrder, DomainError> closeAfterSales(
      String orderNumber, long afterSalesUserId) {
    Result<EscortDispatchOrder, DomainError> orderResult = findOrder(orderNumber);
    if (orderResult.isErr()) {
      return orderResult;
    }

    EscortDispatchOrder order = orderResult.getValue();
    if (!order.isAfterSalesInProgress()) {
      return Result.err(DomainError.invalidInput("此售後案件目前不可結案"));
    }

    if (!order.isAfterSalesAssignee(afterSalesUserId)) {
      return Result.err(DomainError.invalidInput("只有接手此案件的售後人員可以結案"));
    }

    try {
      Optional<EscortDispatchOrder> closed =
          repository.closeAfterSales(order.orderNumber(), afterSalesUserId, now());
      if (closed.isPresent()) {
        return Result.ok(closed.get());
      }
      return Result.err(DomainError.invalidInput("此售後案件目前不可結案"));
    } catch (Exception e) {
      LOG.error(
          "Failed to close after-sales case: orderNumber={}, afterSalesUserId={}",
          order.orderNumber(),
          afterSalesUserId,
          e);
      return Result.err(DomainError.persistenceFailure("售後結案失敗", e));
    }
  }

  /** 依訂單編號查詢。 */
  public Optional<EscortDispatchOrder> findByOrderNumber(String orderNumber) {
    Result<EscortDispatchOrder, DomainError> result = findOrder(orderNumber);
    return result.isOk() ? Optional.of(result.getValue()) : Optional.empty();
  }

  /** 查詢最近訂單（預設 10 筆）。 */
  public Result<List<EscortDispatchOrder>, DomainError> findRecentOrders(
      long guildId, Integer limit) {
    int safeLimit = normalizeLimit(limit);
    try {
      List<EscortDispatchOrder> orders = repository.findRecentByGuildId(guildId, safeLimit);
      List<EscortDispatchOrder> normalizedOrders = new ArrayList<>(orders.size());
      for (EscortDispatchOrder order : orders) {
        normalizedOrders.add(ensureTimeoutCompletion(order));
      }
      return Result.ok(normalizedOrders);
    } catch (Exception e) {
      LOG.error(
          "Failed to query recent escort dispatch orders: guildId={}, limit={}",
          guildId,
          safeLimit,
          e);
      return Result.err(DomainError.persistenceFailure("查詢歷史訂單失敗", e));
    }
  }

  /** 查詢尚未指定護航者的自動交接訂單（預設 5 筆）。 */
  public Result<List<EscortDispatchOrder>, DomainError> findPendingAssignmentOrders(
      long guildId, Integer limit) {
    int safeLimit = normalizeLimit(limit, DEFAULT_PENDING_ASSIGNMENT_LIMIT);
    try {
      List<EscortDispatchOrder> orders =
          repository.findPendingAssignmentByGuildId(guildId, safeLimit);
      return Result.ok(orders);
    } catch (Exception e) {
      LOG.error(
          "Failed to query pending assignment escort dispatch orders: guildId={}, limit={}",
          guildId,
          safeLimit,
          e);
      return Result.err(DomainError.persistenceFailure("查詢待派單訂單失敗", e));
    }
  }

  private Result<EscortDispatchOrder, DomainError> findOrder(String orderNumber) {
    if (orderNumber == null || orderNumber.isBlank()) {
      return Result.err(DomainError.invalidInput("訂單編號無效"));
    }

    String normalizedOrderNumber = orderNumber.trim().toUpperCase();
    try {
      Optional<EscortDispatchOrder> orderOpt = repository.findByOrderNumber(normalizedOrderNumber);
      if (orderOpt.isEmpty()) {
        return Result.err(DomainError.invalidInput("找不到該訂單"));
      }
      EscortDispatchOrder order = ensureTimeoutCompletion(orderOpt.get());
      return Result.ok(order);
    } catch (Exception e) {
      LOG.error("Failed to query escort dispatch order: orderNumber={}", normalizedOrderNumber, e);
      return Result.err(DomainError.persistenceFailure("查詢訂單失敗", e));
    }
  }

  private EscortDispatchOrder ensureTimeoutCompletion(EscortDispatchOrder order) {
    if (!order.hasCustomerConfirmationTimedOut(now())) {
      return order;
    }

    try {
      EscortDispatchOrder completed = repository.update(order.withCompleted(now()));
      LOG.info(
          "Auto-completed escort dispatch order after customer confirmation timeout:"
              + " orderNumber={}",
          completed.orderNumber());
      return completed;
    } catch (Exception e) {
      LOG.warn(
          "Failed to auto-complete timed-out escort dispatch order: orderNumber={}",
          order.orderNumber(),
          e);
      return order;
    }
  }

  private int normalizeLimit(Integer limit) {
    return normalizeLimit(limit, DEFAULT_HISTORY_LIMIT);
  }

  private int normalizeLimit(Integer limit, int defaultLimit) {
    if (limit == null) {
      return defaultLimit;
    }
    if (limit <= 0) {
      return defaultLimit;
    }
    return Math.min(limit, MAX_HISTORY_LIMIT);
  }

  private Instant now() {
    return Instant.now(clock);
  }

  private Result<String, DomainError> normalizeEscortOption(String escortOptionCode) {
    if (escortOptionCode == null || escortOptionCode.isBlank()) {
      return Result.err(DomainError.invalidInput("請選擇護航品類"));
    }
    String normalizedCode = escortOptionCode.trim().toUpperCase();
    if (!EscortOrderOptionCatalog.isSupported(normalizedCode)) {
      return Result.err(
          DomainError.invalidInput(
              "護航品類無效，可用代碼：" + EscortOrderOptionCatalog.supportedCodesForDisplay()));
    }
    return Result.ok(normalizedCode);
  }

  private String generateUniqueOrderNumber() {
    for (int i = 0; i < MAX_ORDER_NUMBER_RETRIES; i++) {
      String candidate = orderNumberGenerator.generate();
      if (!repository.existsByOrderNumber(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to generate unique order number after retries");
  }
}
