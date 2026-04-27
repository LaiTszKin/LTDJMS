package ltdjms.discord.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FiatOrder 測試")
class FiatOrderTest {

  @Test
  @DisplayName("建立待付款訂單時應保存到期時間")
  void createPendingShouldKeepExpireAt() {
    Instant expireAt = Instant.parse("2026-04-11T12:00:00Z");

    FiatOrder order =
        FiatOrder.createPending(
            1L, 2L, 3L, "商品", "FD260411000001", "ABC123456789", 1200L, expireAt);

    assertThat(order.status()).isEqualTo(FiatOrder.Status.PENDING_PAYMENT);
    assertThat(order.expireAt()).isEqualTo(expireAt);
    assertThat(order.isTerminal()).isFalse();
  }

  @Test
  @DisplayName("逾期訂單必須帶有終止時間與原因")
  void expiredOrderShouldRequireTerminalDetails() {
    Instant expireAt = Instant.parse("2026-04-11T12:00:00Z");
    Instant expiredAt = Instant.parse("2026-04-11T12:05:00Z");

    FiatOrder order =
        new FiatOrder(
            1L,
            1L,
            2L,
            3L,
            "商品",
            "FD260411000001",
            "ABC123456789",
            1200L,
            FiatOrder.Status.EXPIRED,
            "0",
            "尚未付款",
            null,
            expireAt,
            expiredAt,
            "EXPIRED",
            null,
            null,
            null,
            null,
            null,
            0,
            null,
            expiredAt.minusSeconds(300),
            expiredAt);

    assertThat(order.isExpired()).isTrue();
    assertThat(order.isTerminal()).isTrue();
    assertThat(order.expiredAt()).isEqualTo(expiredAt);
    assertThat(order.terminalReason()).isEqualTo("EXPIRED");
  }

  @Test
  @DisplayName("逾期訂單缺少終止原因時應被拒絕")
  void expiredOrderShouldRejectMissingTerminalReason() {
    Instant expireAt = Instant.parse("2026-04-11T12:00:00Z");
    Instant expiredAt = Instant.parse("2026-04-11T12:05:00Z");

    assertThatThrownBy(
            () ->
                new FiatOrder(
                    1L,
                    1L,
                    2L,
                    3L,
                    "商品",
                    "FD260411000001",
                    "ABC123456789",
                    1200L,
                    FiatOrder.Status.EXPIRED,
                    "0",
                    "尚未付款",
                    null,
                    expireAt,
                    expiredAt,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null,
                    expiredAt.minusSeconds(300),
                    expiredAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("terminalReason");
  }
}
