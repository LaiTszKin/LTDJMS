package ltdjms.discord.shop.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shop.domain.FiatOrder;
import ltdjms.discord.shop.domain.FiatOrderRepository;

/** Reconciles pending fiat orders against ECPay query API when callbacks are missing. */
public class FiatPaymentReconciliationService {

  private static final Logger LOG = LoggerFactory.getLogger(FiatPaymentReconciliationService.class);
  private static final int DEFAULT_BATCH_SIZE = 20;
  private static final Duration RECONCILIATION_WINDOW = Duration.ofDays(7);
  private static final String EXPIRED_TERMINAL_REASON = "EXPIRED";

  private final FiatOrderRepository fiatOrderRepository;
  private final EcpayTradeQueryService ecpayTradeQueryService;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public FiatPaymentReconciliationService(
      FiatOrderRepository fiatOrderRepository, EcpayTradeQueryService ecpayTradeQueryService) {
    this(fiatOrderRepository, ecpayTradeQueryService, Clock.systemUTC(), new ObjectMapper());
  }

  FiatPaymentReconciliationService(
      FiatOrderRepository fiatOrderRepository,
      EcpayTradeQueryService ecpayTradeQueryService,
      Clock clock,
      ObjectMapper objectMapper) {
    this.fiatOrderRepository = Objects.requireNonNull(fiatOrderRepository);
    this.ecpayTradeQueryService = Objects.requireNonNull(ecpayTradeQueryService);
    this.clock = Objects.requireNonNull(clock);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  public void reconcilePendingOrders() {
    Instant now = Instant.now(clock);
    expirePendingOrders(now);
    List<FiatOrder> orders =
        fiatOrderRepository.findOrdersPendingReconciliation(
            now, now.minus(RECONCILIATION_WINDOW), DEFAULT_BATCH_SIZE);
    for (FiatOrder order : orders) {
      reconcileSingleOrder(order, now);
    }
  }

  void expirePendingOrders(Instant now) {
    List<FiatOrder> orders = fiatOrderRepository.findOrdersPendingExpiry(now, DEFAULT_BATCH_SIZE);
    for (FiatOrder order : orders) {
      fiatOrderRepository.markExpiredIfPending(order.orderNumber(), now, EXPIRED_TERMINAL_REASON);
    }
  }

  void reconcileSingleOrder(FiatOrder order, Instant now) {
    if (!fiatOrderRepository.claimReconciliationProcessing(order.orderNumber(), now)) {
      return;
    }

    try {
      Result<EcpayTradeQueryService.QueryTradeResult, DomainError> queryResult =
          ecpayTradeQueryService.queryTrade(order.orderNumber());
      if (queryResult.isErr()) {
        scheduleRetry(order, now);
        return;
      }

      EcpayTradeQueryService.QueryTradeResult trade = queryResult.getValue();
      if (!trade.paid()) {
        Instant decisionTime = Instant.now(clock);
        if (!decisionTime.isBefore(order.expireAt())) {
          fiatOrderRepository.markExpiredIfPending(
              order.orderNumber(), decisionTime, EXPIRED_TERMINAL_REASON);
        } else {
          scheduleRetry(order, decisionTime);
        }
        return;
      }

      if (fiatOrderRepository
          .markPaidIfPending(
              order.orderNumber(),
              trade.tradeStatus(),
              trade.message(),
              buildSyntheticPayload(trade),
              now)
          .isEmpty()) {
        fiatOrderRepository.releaseReconciliationProcessing(order.orderNumber());
      }
    } catch (Exception e) {
      fiatOrderRepository.releaseReconciliationProcessing(order.orderNumber());
      LOG.warn("Failed to reconcile fiat order: orderNumber={}", order.orderNumber(), e);
    }
  }

  private void scheduleRetry(FiatOrder order, Instant now) {
    int nextAttempt = order.reconciliationAttemptCount() + 1;
    Instant nextAttemptAt = now.plusSeconds(Math.min(300L, 30L * nextAttempt));
    fiatOrderRepository.markReconciliationAttempted(
        order.orderNumber(), nextAttempt, nextAttemptAt);
  }

  private String buildSyntheticPayload(EcpayTradeQueryService.QueryTradeResult result) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("source", "ECPAY_QUERY_TRADE_INFO");
    root.put("orderNumber", result.orderNumber());
    root.put("tradeStatus", result.tradeStatus());
    if (result.tradeNo() != null) {
      root.put("tradeNo", result.tradeNo());
    }
    if (result.tradeAmount() >= 0) {
      root.put("tradeAmt", result.tradeAmount());
    }
    if (result.message() != null) {
      root.put("message", result.message());
    }
    return root.toString();
  }
}
