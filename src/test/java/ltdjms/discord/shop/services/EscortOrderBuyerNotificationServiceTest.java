package ltdjms.discord.shop.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.shared.runtime.DiscordRuntimeGateway;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscortOrderBuyerNotificationService 測試")
class EscortOrderBuyerNotificationServiceTest {

  private static final long BUYER_USER_ID = 123456789L;

  @Mock private DiscordRuntimeGateway discordRuntimeGateway;
  @Mock private RestAction<User> retrieveUserAction;
  @Mock private User buyerUser;
  @Mock private CacheRestAction<PrivateChannel> openPrivateChannelAction;
  @Mock private PrivateChannel privateChannel;
  @Mock private MessageCreateAction messageCreateAction;

  private EscortOrderBuyerNotificationService service;

  @BeforeEach
  void setUp() {
    service = new EscortOrderBuyerNotificationService(discordRuntimeGateway);
  }

  private EscortDispatchOrder createOrder(EscortDispatchOrder.SourceType sourceType) {
    return EscortDispatchOrder.createAutoHandoff(
        "ESC-20260428-ABC123",
        999L,
        0L,
        0L,
        BUYER_USER_ID,
        sourceType,
        "REF-001",
        1L,
        "VIP 護航方案",
        sourceType == EscortDispatchOrder.SourceType.CURRENCY_PURCHASE ? 100000L : null,
        sourceType == EscortDispatchOrder.SourceType.FIAT_PAYMENT ? 1200L : null,
        "escort-a");
  }

  private void stubSuccessfulDm() {
    when(discordRuntimeGateway.retrieveUserById(BUYER_USER_ID)).thenReturn(retrieveUserAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<User> success = invocation.getArgument(0);
              success.accept(buyerUser);
              return null;
            })
        .when(retrieveUserAction)
        .queue(any(), any());
    when(buyerUser.openPrivateChannel()).thenReturn(openPrivateChannelAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<PrivateChannel> success = invocation.getArgument(0);
              success.accept(privateChannel);
              return null;
            })
        .when(openPrivateChannelAction)
        .queue(any(), any());
    when(privateChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<Void> success = invocation.getArgument(0);
              success.accept(null);
              return null;
            })
        .when(messageCreateAction)
        .queue(any(), any());
  }

  @Test
  @DisplayName("UT-01: 應使用正確的 customerUserId 呼叫 retrieveUserById")
  void shouldRetrieveUserWithCorrectCustomerUserId() {
    stubSuccessfulDm();
    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);

    service.notifyEscortOrderCreated(order);

    verify(discordRuntimeGateway).retrieveUserById(BUYER_USER_ID);
    verify(privateChannel).sendMessage(anyString());
  }

  @Test
  @DisplayName("UT-02: 貨幣購買通知應包含商品名稱、訂單編號、等待處理、貨幣")
  void currencyPurchaseMessage_shouldContainExpectedKeywords() {
    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);

    String message = service.buildEscortOrderCreatedMessage(order);

    assertThat(message)
        .contains("VIP 護航方案")
        .contains("ESC-20260428-ABC123")
        .contains("等待處理")
        .contains("貨幣")
        .doesNotContain("法幣");
  }

  @Test
  @DisplayName("UT-02: 法幣付款通知應包含商品名稱、訂單編號、等待處理、法幣")
  void fiatPaymentMessage_shouldContainExpectedKeywords() {
    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.FIAT_PAYMENT);

    String message = service.buildEscortOrderCreatedMessage(order);

    assertThat(message)
        .contains("VIP 護航方案")
        .contains("ESC-20260428-ABC123")
        .contains("等待處理")
        .contains("法幣")
        .doesNotContain("貨幣");
  }

  @Test
  @DisplayName("UT-06: DM 關閉時不拋例外")
  void shouldNotThrowWhenDmFails() {
    when(discordRuntimeGateway.retrieveUserById(BUYER_USER_ID)).thenReturn(retrieveUserAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<User> success = invocation.getArgument(0);
              success.accept(buyerUser);
              return null;
            })
        .when(retrieveUserAction)
        .queue(any(), any());
    when(buyerUser.openPrivateChannel()).thenThrow(new RuntimeException("DM blocked"));

    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);

    service.notifyEscortOrderCreated(order);
  }

  @Test
  @DisplayName("order 為 null 時不應拋例外")
  void shouldNotThrowWhenOrderIsNull() {
    service.notifyEscortOrderCreated(null);
    verify(discordRuntimeGateway, never()).retrieveUserById(anyLong());
  }

  @Test
  @DisplayName("UT-01: 應發送訊息到私訊頻道")
  void shouldSendMessageToPrivateChannel() {
    stubSuccessfulDm();
    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);

    service.notifyEscortOrderCreated(order);

    verify(privateChannel).sendMessage(anyString());
  }

  @Test
  @DisplayName("retrieveUser 失敗時應優雅處理")
  void shouldHandleRetrieveUserFailureGracefully() {
    when(discordRuntimeGateway.retrieveUserById(BUYER_USER_ID)).thenReturn(retrieveUserAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<Throwable> failure = invocation.getArgument(1);
              failure.accept(new RuntimeException("User not found"));
              return null;
            })
        .when(retrieveUserAction)
        .queue(any(), any());

    EscortDispatchOrder order = createOrder(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);

    service.notifyEscortOrderCreated(order);
  }
}
