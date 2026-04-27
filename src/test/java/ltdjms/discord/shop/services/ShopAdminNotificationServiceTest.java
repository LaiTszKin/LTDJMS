package ltdjms.discord.shop.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.shared.runtime.DiscordRuntimeGateway;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopAdminNotificationService 測試")
class ShopAdminNotificationServiceTest {

  private static final long GUILD_ID = 123456789L;
  private static final long BOT_USER_ID = 900L;
  private static final long ADMIN_USER_ID = 901L;
  private static final long SECOND_ADMIN_USER_ID = 902L;
  private static final long BUYER_USER_ID = 987654321L;

  @Mock private DiscordRuntimeGateway discordRuntimeGateway;
  @Mock private Guild guild;
  @Mock private Member botMember;
  @Mock private Member adminMember;
  @Mock private Member secondAdminMember;
  @Mock private User botUser;
  @Mock private User adminUser;
  @Mock private User secondAdminUser;
  @Mock private CacheRestAction<PrivateChannel> openPrivateChannelAction;
  @Mock private PrivateChannel privateChannel;
  @Mock private MessageCreateAction messageCreateAction;

  private ShopAdminNotificationService service;

  @BeforeEach
  void setUp() {
    service = new ShopAdminNotificationService(discordRuntimeGateway);

    when(discordRuntimeGateway.getGuildById(GUILD_ID)).thenReturn(guild);
    when(discordRuntimeGateway.getSelfUserId()).thenReturn(BOT_USER_ID);
    when(guild.getIdLong()).thenReturn(GUILD_ID);
    when(guild.getName()).thenReturn("Test Guild");
    when(guild.getOwnerIdLong()).thenReturn(BOT_USER_ID);
  }

  @Test
  @DisplayName("通知時應略過 bot 自己")
  void shouldSkipBotSelfWhenNotifyingAdmins() {
    when(guild.getMembers()).thenReturn(List.of(botMember, adminMember));

    when(botMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
    when(botMember.getUser()).thenReturn(botUser);
    when(botUser.getIdLong()).thenReturn(BOT_USER_ID);

    when(adminMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
    when(adminMember.getUser()).thenReturn(adminUser);
    when(adminUser.getIdLong()).thenReturn(ADMIN_USER_ID);
    stubSuccessfulDirectMessage(adminUser);

    Product product =
        new Product(
            1L,
            GUILD_ID,
            "Test Product",
            null,
            null,
            null,
            100L,
            null,
            true,
            "escort-a",
            Instant.parse("2026-04-27T00:00:00Z"),
            Instant.parse("2026-04-27T00:00:00Z"));

    service.notifyAdminsOrderCreated(GUILD_ID, BUYER_USER_ID, product, "購買", "ORD-001");

    verify(botUser, never()).openPrivateChannel();
    verify(adminUser).openPrivateChannel();
  }

  @Test
  @DisplayName("單一管理員 DM 失敗不應中斷其他通知")
  void shouldContinueWhenOneAdminDmFails() {
    when(guild.getMembers()).thenReturn(List.of(adminMember, secondAdminMember));
    when(adminMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
    when(adminMember.getUser()).thenReturn(adminUser);
    when(adminUser.getIdLong()).thenReturn(ADMIN_USER_ID);
    when(secondAdminMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
    when(secondAdminMember.getUser()).thenReturn(secondAdminUser);
    when(secondAdminUser.getIdLong()).thenReturn(SECOND_ADMIN_USER_ID);
    stubSuccessfulDirectMessage(secondAdminUser);
    doThrow(new UnsupportedOperationException("DM blocked")).when(adminUser).openPrivateChannel();

    EscortDispatchOrder order =
        EscortDispatchOrder.createAutoHandoff(
            "ESC-20260427-ABC123",
            GUILD_ID,
            0L,
            0L,
            BUYER_USER_ID,
            EscortDispatchOrder.SourceType.CURRENCY_PURCHASE,
            "ORDER-001",
            1L,
            "Test Product",
            100L,
            null,
            "escort-a");

    assertThatCode(() -> service.notifyAdminsOrderCreated(GUILD_ID, BUYER_USER_ID, order))
        .doesNotThrowAnyException();

    verify(adminUser).openPrivateChannel();
    verify(secondAdminUser).openPrivateChannel();
  }

  private void stubSuccessfulDirectMessage(User user) {
    when(user.openPrivateChannel()).thenReturn(openPrivateChannelAction);
    when(privateChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Consumer<PrivateChannel> success = invocation.getArgument(0);
              success.accept(privateChannel);
              return null;
            })
        .when(openPrivateChannelAction)
        .queue(any(), any());
  }
}
