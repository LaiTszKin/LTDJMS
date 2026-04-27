package ltdjms.discord.dispatch.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.domain.EscortDispatchOrderRepository;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** Durable handoff boundary for auto-created escort dispatch work items. */
public class EscortDispatchHandoffService {

  private static final Logger LOG = LoggerFactory.getLogger(EscortDispatchHandoffService.class);
  private static final int MAX_ORDER_NUMBER_RETRIES = 20;

  private final EscortDispatchOrderRepository repository;
  private final EscortDispatchOrderNumberGenerator orderNumberGenerator;

  public EscortDispatchHandoffService(EscortDispatchOrderRepository repository) {
    this(repository, new EscortDispatchOrderNumberGenerator());
  }

  EscortDispatchHandoffService(
      EscortDispatchOrderRepository repository,
      EscortDispatchOrderNumberGenerator orderNumberGenerator) {
    this.repository = repository;
    this.orderNumberGenerator = orderNumberGenerator;
  }

  public Result<EscortDispatchOrder, DomainError> handoffFromCurrencyPurchase(
      long guildId, long buyerUserId, Product product, String sourceReference) {
    return handoff(
        guildId,
        buyerUserId,
        product,
        sourceReference,
        EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);
  }

  public Result<EscortDispatchOrder, DomainError> handoffFromFiatPayment(
      long guildId, long buyerUserId, Product product, String sourceReference) {
    return handoff(
        guildId,
        buyerUserId,
        product,
        sourceReference,
        EscortDispatchOrder.SourceType.FIAT_PAYMENT);
  }

  private Result<EscortDispatchOrder, DomainError> handoff(
      long guildId,
      long buyerUserId,
      Product product,
      String sourceReference,
      EscortDispatchOrder.SourceType sourceType) {
    try {
      if (product == null) {
        return Result.err(DomainError.invalidInput("找不到該商品"));
      }
      if (!product.shouldAutoCreateEscortOrder()) {
        return Result.err(DomainError.invalidInput("此商品尚未啟用自動護航開單"));
      }
      if (sourceReference == null || sourceReference.isBlank()) {
        return Result.err(DomainError.invalidInput("來源參考無效"));
      }
      if (product.escortOptionCode() == null || product.escortOptionCode().isBlank()) {
        return Result.err(DomainError.invalidInput("護航選項代碼無效"));
      }

      Optional<EscortDispatchOrder> existing =
          repository.findBySourceIdentity(sourceType, sourceReference);
      if (existing.isPresent()) {
        return Result.ok(existing.get());
      }

      EscortDispatchOrder order =
          EscortDispatchOrder.createAutoHandoff(
              generateUniqueOrderNumber(),
              guildId,
              0L,
              0L,
              buyerUserId,
              sourceType,
              sourceReference,
              product.id(),
              product.name(),
              product.currencyPrice(),
              product.fiatPriceTwd(),
              product.escortOptionCode());
      EscortDispatchOrder saved = repository.save(order);
      LOG.info(
          "Created escort dispatch handoff: orderNumber={}, sourceType={}, sourceReference={}",
          saved.orderNumber(),
          sourceType,
          sourceReference);
      return Result.ok(saved);
    } catch (Exception e) {
      LOG.error(
          "Failed to create escort dispatch handoff: guildId={}, buyerUserId={}, sourceType={},"
              + " sourceReference={}",
          guildId,
          buyerUserId,
          sourceType,
          sourceReference,
          e);

      Optional<EscortDispatchOrder> fallback =
          repository.findBySourceIdentity(sourceType, sourceReference);
      if (fallback.isPresent()) {
        return Result.ok(fallback.get());
      }
      return Result.err(DomainError.persistenceFailure("建立護航交接失敗", e));
    }
  }

  private String generateUniqueOrderNumber() {
    for (int attempt = 1; attempt <= MAX_ORDER_NUMBER_RETRIES; attempt++) {
      String orderNumber = orderNumberGenerator.generate();
      if (!repository.existsByOrderNumber(orderNumber)) {
        return orderNumber;
      }
    }
    throw new IllegalStateException("無法產生唯一的護航訂單編號");
  }
}
