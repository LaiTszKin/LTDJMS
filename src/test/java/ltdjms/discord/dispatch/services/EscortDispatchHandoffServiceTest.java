package ltdjms.discord.dispatch.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.domain.EscortDispatchOrderRepository;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscortDispatchHandoffService 測試")
class EscortDispatchHandoffServiceTest {

  private static final long TEST_GUILD_ID = 123456789L;
  private static final long TEST_BUYER_ID = 987654321L;

  @Mock private EscortDispatchOrderRepository repository;
  @Mock private EscortDispatchOrderNumberGenerator orderNumberGenerator;

  private EscortDispatchHandoffService service;

  @BeforeEach
  void setUp() {
    service = new EscortDispatchHandoffService(repository, orderNumberGenerator);
  }

  @Test
  @DisplayName("貨幣購買交接應建立帶有來源快照的 dispatch work item")
  void shouldCreateCurrencyHandoffWithSnapshot() {
    Product product = autoEscortProduct(100L, null, "CONF_HOURLY_1H");
    when(repository.findBySourceIdentity(
            EscortDispatchOrder.SourceType.CURRENCY_PURCHASE, "interaction-123"))
        .thenReturn(Optional.empty());
    when(orderNumberGenerator.generate()).thenReturn("ESC-20260411-ABC123");
    when(repository.existsByOrderNumber("ESC-20260411-ABC123")).thenReturn(false);
    when(repository.save(any(EscortDispatchOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Result<EscortDispatchOrder, DomainError> result =
        service.handoffFromCurrencyPurchase(
            TEST_GUILD_ID, TEST_BUYER_ID, product, "interaction-123");

    assertThat(result.isOk()).isTrue();
    EscortDispatchOrder order = result.getValue();
    assertThat(order.sourceType()).isEqualTo(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);
    assertThat(order.sourceReference()).isEqualTo("interaction-123");
    assertThat(order.sourceProductId()).isEqualTo(product.id());
    assertThat(order.sourceProductName()).isEqualTo(product.name());
    assertThat(order.sourceCurrencyPrice()).isEqualTo(100L);
    assertThat(order.sourceFiatPriceTwd()).isNull();
    assertThat(order.sourceEscortOptionCode()).isEqualTo("CONF_HOURLY_1H");
    verify(repository).save(any(EscortDispatchOrder.class));
  }

  @Test
  @DisplayName("相同來源參考重放時應回傳既有 work item")
  void shouldReturnExistingHandoffWhenSourceIdentityExists() {
    Product product = autoEscortProduct(100L, null, "CONF_HOURLY_1H");
    EscortDispatchOrder existing =
        EscortDispatchOrder.createAutoHandoff(
            "ESC-20260411-ABC123",
            TEST_GUILD_ID,
            0L,
            0L,
            TEST_BUYER_ID,
            EscortDispatchOrder.SourceType.CURRENCY_PURCHASE,
            "interaction-123",
            product.id(),
            product.name(),
            product.currencyPrice(),
            product.fiatPriceTwd(),
            product.escortOptionCode());

    when(repository.findBySourceIdentity(
            EscortDispatchOrder.SourceType.CURRENCY_PURCHASE, "interaction-123"))
        .thenReturn(Optional.of(existing));

    Result<EscortDispatchOrder, DomainError> result =
        service.handoffFromCurrencyPurchase(
            TEST_GUILD_ID, TEST_BUYER_ID, product, "interaction-123");

    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue()).isSameAs(existing);
    verify(repository, never()).save(any());
    verify(repository, never()).existsByOrderNumber(any());
  }

  private Product autoEscortProduct(
      Long currencyPrice, Long fiatPriceTwd, String escortOptionCode) {
    return new Product(
        1000L,
        TEST_GUILD_ID,
        "Auto Escort Product",
        "desc",
        null,
        null,
        currencyPrice,
        fiatPriceTwd,
        true,
        escortOptionCode,
        Instant.parse("2026-04-11T09:00:00Z"),
        Instant.parse("2026-04-11T09:00:00Z"));
  }
}
