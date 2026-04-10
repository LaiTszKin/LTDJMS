package ltdjms.discord.shop.services;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Periodically runs fiat order post-payment processing and callback reconciliation. */
public class FiatOrderProcessingScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(FiatOrderProcessingScheduler.class);
  private static final long POST_PAYMENT_INTERVAL_SECONDS = 10L;
  private static final long RECONCILIATION_INTERVAL_SECONDS = 60L;

  private final FiatOrderPostPaymentWorker postPaymentWorker;
  private final FiatPaymentReconciliationService reconciliationService;
  private ScheduledExecutorService executorService;

  public FiatOrderProcessingScheduler(
      FiatOrderPostPaymentWorker postPaymentWorker,
      FiatPaymentReconciliationService reconciliationService) {
    this.postPaymentWorker = Objects.requireNonNull(postPaymentWorker);
    this.reconciliationService = Objects.requireNonNull(reconciliationService);
  }

  public synchronized void start() {
    if (executorService != null) {
      return;
    }
    executorService = Executors.newScheduledThreadPool(2);
    executorService.scheduleWithFixedDelay(
        this::runPostPayment, 2L, POST_PAYMENT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    executorService.scheduleWithFixedDelay(
        this::runReconciliation, 5L, RECONCILIATION_INTERVAL_SECONDS, TimeUnit.SECONDS);
    LOG.info("Started fiat order processing scheduler");
  }

  public synchronized void stop() {
    if (executorService == null) {
      return;
    }
    executorService.shutdownNow();
    executorService = null;
    LOG.info("Stopped fiat order processing scheduler");
  }

  private void runPostPayment() {
    try {
      postPaymentWorker.processPendingOrders();
    } catch (Exception e) {
      LOG.warn("Fiat post-payment worker tick failed", e);
    }
  }

  private void runReconciliation() {
    try {
      reconciliationService.reconcilePendingOrders();
    } catch (Exception e) {
      LOG.warn("Fiat reconciliation worker tick failed", e);
    }
  }
}
