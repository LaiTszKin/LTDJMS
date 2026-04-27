package ltdjms.discord.dispatch.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.domain.EscortDispatchOrderRepository;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

@ExtendWith(MockitoExtension.class)
class EscortDispatchOrderServiceTest {

  private static final long TEST_GUILD_ID = 123456789L;
  private static final long TEST_ADMIN_ID = 10001L;
  private static final long TEST_ESCORT_USER_ID = 40001L;
  private static final long TEST_CUSTOMER_USER_ID = 50001L;
  private static final Instant FIXED_NOW = Instant.parse("2026-02-14T12:00:00Z");

  @Mock private EscortDispatchOrderRepository repository;
  @Mock private EscortDispatchOrderNumberGenerator orderNumberGenerator;

  private EscortDispatchOrderService service;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    service = new EscortDispatchOrderService(repository, orderNumberGenerator, fixedClock);
  }

  @Nested
  @DisplayName("createOrder")
  class CreateOrderTests {

    @Test
    @DisplayName("should reject when escort and customer are the same user")
    void shouldRejectWhenEscortAndCustomerAreTheSameUser() {
      Result<EscortDispatchOrder, DomainError> result =
          service.createOrder(
              TEST_GUILD_ID, TEST_ADMIN_ID, TEST_ESCORT_USER_ID, TEST_ESCORT_USER_ID);

      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().category()).isEqualTo(DomainError.Category.INVALID_INPUT);
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("should create order successfully")
    void shouldCreateOrderSuccessfully() {
      when(orderNumberGenerator.generate()).thenReturn("ESC-20260214-ABC123");
      when(repository.existsByOrderNumber("ESC-20260214-ABC123")).thenReturn(false);

      EscortDispatchOrder savedOrder =
          buildOrder(
              EscortDispatchOrder.Status.PENDING_CONFIRMATION,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T10:00:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T10:00:00Z"));
      when(repository.save(any(EscortDispatchOrder.class))).thenReturn(savedOrder);

      Result<EscortDispatchOrder, DomainError> result =
          service.createOrder(
              TEST_GUILD_ID, TEST_ADMIN_ID, TEST_ESCORT_USER_ID, TEST_CUSTOMER_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().orderNumber()).isEqualTo("ESC-20260214-ABC123");
    }
  }

  @Nested
  @DisplayName("confirmOrder")
  class ConfirmOrderTests {

    @Test
    @DisplayName("should confirm order successfully")
    void shouldConfirmOrderSuccessfully() {
      EscortDispatchOrder pendingOrder =
          buildOrder(
              EscortDispatchOrder.Status.PENDING_CONFIRMATION,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T09:00:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(pendingOrder));
      when(repository.update(any(EscortDispatchOrder.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Result<EscortDispatchOrder, DomainError> result =
          service.confirmOrder("ESC-20260214-ABC123", TEST_ESCORT_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status()).isEqualTo(EscortDispatchOrder.Status.CONFIRMED);
      assertThat(result.getValue().confirmedAt()).isEqualTo(FIXED_NOW);
    }
  }

  @Nested
  @DisplayName("completion flow")
  class CompletionFlowTests {

    @Test
    @DisplayName("should request completion by escort")
    void shouldRequestCompletionByEscort() {
      EscortDispatchOrder confirmedOrder =
          buildOrder(
              EscortDispatchOrder.Status.CONFIRMED,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T09:10:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(confirmedOrder));
      when(repository.update(any(EscortDispatchOrder.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Result<EscortDispatchOrder, DomainError> result =
          service.requestCompletion("ESC-20260214-ABC123", TEST_ESCORT_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status())
          .isEqualTo(EscortDispatchOrder.Status.PENDING_CUSTOMER_CONFIRMATION);
      assertThat(result.getValue().completionRequestedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("should allow customer to confirm completion")
    void shouldAllowCustomerToConfirmCompletion() {
      EscortDispatchOrder pendingCustomerOrder =
          buildOrder(
              EscortDispatchOrder.Status.PENDING_CUSTOMER_CONFIRMATION,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T11:30:00Z"),
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T11:30:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(pendingCustomerOrder));
      when(repository.update(any(EscortDispatchOrder.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Result<EscortDispatchOrder, DomainError> result =
          service.customerConfirmCompletion("ESC-20260214-ABC123", TEST_CUSTOMER_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status()).isEqualTo(EscortDispatchOrder.Status.COMPLETED);
      assertThat(result.getValue().completedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("should auto-complete order when customer confirmation timed out")
    void shouldAutoCompleteOrderWhenCustomerConfirmationTimedOut() {
      EscortDispatchOrder pendingCustomerOrder =
          buildOrder(
              EscortDispatchOrder.Status.PENDING_CUSTOMER_CONFIRMATION,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-13T00:00:00Z"),
              Instant.parse("2026-02-13T00:10:00Z"),
              Instant.parse("2026-02-13T11:00:00Z"),
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-13T11:00:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(pendingCustomerOrder));
      when(repository.update(any(EscortDispatchOrder.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Result<EscortDispatchOrder, DomainError> result =
          service.customerConfirmCompletion("ESC-20260214-ABC123", TEST_CUSTOMER_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status()).isEqualTo(EscortDispatchOrder.Status.COMPLETED);
      assertThat(result.getValue().completedAt()).isEqualTo(FIXED_NOW);
    }
  }

  @Nested
  @DisplayName("after-sales flow")
  class AfterSalesFlowTests {

    @Test
    @DisplayName("should request after-sales from customer")
    void shouldRequestAfterSalesFromCustomer() {
      EscortDispatchOrder pendingCustomerOrder =
          buildOrder(
              EscortDispatchOrder.Status.PENDING_CUSTOMER_CONFIRMATION,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T11:30:00Z"),
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T11:30:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(pendingCustomerOrder));
      when(repository.update(any(EscortDispatchOrder.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Result<EscortDispatchOrder, DomainError> result =
          service.requestAfterSales("ESC-20260214-ABC123", TEST_CUSTOMER_USER_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status())
          .isEqualTo(EscortDispatchOrder.Status.AFTER_SALES_REQUESTED);
      assertThat(result.getValue().afterSalesRequestedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("should claim after-sales case successfully")
    void shouldClaimAfterSalesCaseSuccessfully() {
      EscortDispatchOrder requestedOrder =
          buildOrder(
              EscortDispatchOrder.Status.AFTER_SALES_REQUESTED,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T10:00:00Z"),
              Instant.parse("2026-02-14T10:05:00Z"),
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T10:05:00Z"));
      EscortDispatchOrder claimedOrder =
          buildOrder(
              EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T10:00:00Z"),
              Instant.parse("2026-02-14T10:05:00Z"),
              TEST_ADMIN_ID,
              Instant.parse("2026-02-14T11:59:00Z"),
              null,
              null,
              Instant.parse("2026-02-14T11:59:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(requestedOrder));
      when(repository.claimAfterSales("ESC-20260214-ABC123", TEST_ADMIN_ID, FIXED_NOW))
          .thenReturn(Optional.of(claimedOrder));

      Result<EscortDispatchOrder, DomainError> result =
          service.claimAfterSales("ESC-20260214-ABC123", TEST_ADMIN_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status())
          .isEqualTo(EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS);
      assertThat(result.getValue().afterSalesAssigneeUserId()).isEqualTo(TEST_ADMIN_ID);
    }

    @Test
    @DisplayName("should reject claim when case already claimed by others")
    void shouldRejectClaimWhenCaseAlreadyClaimedByOthers() {
      EscortDispatchOrder inProgressOrder =
          buildOrder(
              EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T10:00:00Z"),
              Instant.parse("2026-02-14T10:05:00Z"),
              TEST_ADMIN_ID,
              Instant.parse("2026-02-14T11:59:00Z"),
              null,
              null,
              Instant.parse("2026-02-14T11:59:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(inProgressOrder));

      Result<EscortDispatchOrder, DomainError> result =
          service.claimAfterSales("ESC-20260214-ABC123", TEST_ESCORT_USER_ID);

      assertThat(result.isErr()).isTrue();
      assertThat(result.getError().message()).contains("已由其他售後人員接手");
      verify(repository, never()).claimAfterSales(any(), anyLong(), any());
    }

    @Test
    @DisplayName("should close after-sales case by assignee")
    void shouldCloseAfterSalesCaseByAssignee() {
      EscortDispatchOrder inProgressOrder =
          buildOrder(
              EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T10:00:00Z"),
              Instant.parse("2026-02-14T10:05:00Z"),
              TEST_ADMIN_ID,
              Instant.parse("2026-02-14T11:59:00Z"),
              null,
              null,
              Instant.parse("2026-02-14T11:59:00Z"));
      EscortDispatchOrder closedOrder =
          buildOrder(
              EscortDispatchOrder.Status.AFTER_SALES_CLOSED,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              Instant.parse("2026-02-14T10:00:00Z"),
              Instant.parse("2026-02-14T10:05:00Z"),
              TEST_ADMIN_ID,
              Instant.parse("2026-02-14T11:59:00Z"),
              Instant.parse("2026-02-14T12:00:00Z"),
              null,
              Instant.parse("2026-02-14T12:00:00Z"));
      when(repository.findByOrderNumber("ESC-20260214-ABC123"))
          .thenReturn(Optional.of(inProgressOrder));
      when(repository.closeAfterSales("ESC-20260214-ABC123", TEST_ADMIN_ID, FIXED_NOW))
          .thenReturn(Optional.of(closedOrder));

      Result<EscortDispatchOrder, DomainError> result =
          service.closeAfterSales("ESC-20260214-ABC123", TEST_ADMIN_ID);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue().status())
          .isEqualTo(EscortDispatchOrder.Status.AFTER_SALES_CLOSED);
    }
  }

  @Nested
  @DisplayName("history")
  class HistoryTests {

    @Test
    @DisplayName("should query recent orders")
    void shouldQueryRecentOrders() {
      EscortDispatchOrder order =
          buildOrder(
              EscortDispatchOrder.Status.CONFIRMED,
              "ESC-20260214-ABC123",
              Instant.parse("2026-02-14T09:00:00Z"),
              Instant.parse("2026-02-14T09:10:00Z"),
              null,
              null,
              null,
              null,
              null,
              null,
              Instant.parse("2026-02-14T09:10:00Z"));
      when(repository.findRecentByGuildId(TEST_GUILD_ID, 5)).thenReturn(List.of(order));

      Result<List<EscortDispatchOrder>, DomainError> result =
          service.findRecentOrders(TEST_GUILD_ID, 5);

      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).hasSize(1);
      assertThat(result.getValue().get(0).orderNumber()).isEqualTo("ESC-20260214-ABC123");
    }
  }

  private EscortDispatchOrder buildOrder(
      EscortDispatchOrder.Status status,
      String orderNumber,
      Instant createdAt,
      Instant confirmedAt,
      Instant completionRequestedAt,
      Instant afterSalesRequestedAt,
      Long afterSalesAssigneeUserId,
      Instant afterSalesAssignedAt,
      Instant afterSalesClosedAt,
      Instant completedAt,
      Instant updatedAt) {
    return new EscortDispatchOrder(
        1L,
        orderNumber,
        TEST_GUILD_ID,
        TEST_ADMIN_ID,
        TEST_ESCORT_USER_ID,
        TEST_CUSTOMER_USER_ID,
        createdAt,
        confirmedAt,
        completionRequestedAt,
        completedAt,
        afterSalesRequestedAt,
        afterSalesAssigneeUserId,
        afterSalesAssignedAt,
        afterSalesClosedAt,
        updatedAt,
        EscortDispatchOrder.SourceType.MANUAL,
        null,
        null,
        null,
        null,
        null,
        null,
        status);
  }
}
