package ltdjms.discord.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.product.domain.Product;

@DisplayName("FiatOrder 測試")
class FiatOrderTest {

  @Test
  @DisplayName("pending order 應保存完整履約快照")
  void shouldCaptureFulfillmentSnapshot() {
    FiatOrder order =
        FiatOrder.createPending(
            123L,
            456L,
            789L,
            "法幣商品",
            Product.RewardType.CURRENCY,
            50L,
            true,
            "ESCORT-A",
            "FD260411000003",
            "CVS999999",
            1200L);

    assertThat(order.productName()).isEqualTo("法幣商品");
    assertThat(order.fulfillmentRewardType()).isEqualTo(Product.RewardType.CURRENCY);
    assertThat(order.fulfillmentRewardAmount()).isEqualTo(50L);
    assertThat(order.fulfillmentAutoCreateEscortOrder()).isTrue();
    assertThat(order.fulfillmentEscortOptionCode()).isEqualTo("ESCORT-A");
    assertThat(order.toFulfillmentProduct().name()).isEqualTo("法幣商品");
    assertThat(order.toFulfillmentProduct().rewardType()).isEqualTo(Product.RewardType.CURRENCY);
    assertThat(order.toFulfillmentProduct().rewardAmount()).isEqualTo(50L);
    assertThat(order.toFulfillmentProduct().shouldAutoCreateEscortOrder()).isTrue();
  }

  @Test
  @DisplayName("不完整的履約快照應在建單時失敗")
  void shouldRejectIncompleteSnapshot() {
    assertThatThrownBy(
            () ->
                FiatOrder.createPending(
                    123L,
                    456L,
                    789L,
                    "法幣商品",
                    Product.RewardType.CURRENCY,
                    50L,
                    false,
                    "ESCORT-A",
                    "FD260411000004",
                    "CVS999998",
                    1200L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "fulfillmentEscortOptionCode requires fulfillmentAutoCreateEscortOrder");

    assertThatThrownBy(
            () ->
                FiatOrder.createPending(
                    123L,
                    456L,
                    789L,
                    "法幣商品",
                    Product.RewardType.CURRENCY,
                    null,
                    true,
                    "ESCORT-A",
                    "FD260411000005",
                    "CVS999997",
                    1200L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fulfillmentRewardType and fulfillmentRewardAmount");
  }
}
