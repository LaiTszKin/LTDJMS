package ltdjms.discord.panel.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ltdjms.discord.discord.domain.DiscordSessionManager;
import net.dv8tion.jda.api.interactions.InteractionHook;

@DisplayName("AdminPanelSessionManager 測試")
class AdminPanelSessionManagerTest {

  private FakeDiscordSessionManager backingManager;
  private AdminPanelSessionManager sessionManager;

  @BeforeEach
  void setUp() {
    backingManager = new FakeDiscordSessionManager();
    sessionManager = new AdminPanelSessionManager(backingManager);
  }

  @Test
  @DisplayName("registerSession 應建立 MAIN view 並保留 hook")
  void registerSessionShouldCreateMainViewContext() {
    InteractionHook hook = mock(InteractionHook.class);

    sessionManager.registerSession(100L, 200L, hook);

    Optional<AdminPanelSessionManager.AdminPanelSessionContext> contextOpt =
        sessionManager.getSessionContext(100L, 200L);

    assertThat(contextOpt).isPresent();
    assertThat(contextOpt.get().hook()).isSameAs(hook);
    assertThat(contextOpt.get().currentView())
        .isEqualTo(AdminPanelSessionManager.AdminPanelView.MAIN);
    assertThat(contextOpt.get().metadata()).isEmpty();
  }

  @Test
  @DisplayName("updatePanelsByGuild 應枚舉同 guild 的有效 session 與 metadata")
  void updatePanelsByGuildShouldTraverseGuildSessions() {
    InteractionHook mainHook = mock(InteractionHook.class);
    InteractionHook detailHook = mock(InteractionHook.class);
    InteractionHook otherGuildHook = mock(InteractionHook.class);

    sessionManager.registerSession(100L, 200L, mainHook);
    sessionManager.registerSession(100L, 300L, detailHook);
    sessionManager.registerSession(999L, 400L, otherGuildHook);

    sessionManager.updateSessionView(
        100L, 200L, AdminPanelSessionManager.AdminPanelView.MAIN, Map.of());
    sessionManager.updateSessionView(
        100L,
        300L,
        AdminPanelSessionManager.AdminPanelView.PRODUCT_DETAIL,
        Map.of("productId", 42L, "page", 3));
    sessionManager.updateSessionView(
        999L, 400L, AdminPanelSessionManager.AdminPanelView.PRODUCT_LIST, Map.of("productId", 88L));

    var visitedAdminIds = new java.util.ArrayList<Long>();
    var visitedViews = new java.util.ArrayList<AdminPanelSessionManager.AdminPanelView>();
    var visitedMetadata = new java.util.ArrayList<Map<String, Object>>();

    sessionManager.updatePanelsByGuild(
        100L,
        context -> {
          visitedAdminIds.add(context.adminId());
          visitedViews.add(context.currentView());
          visitedMetadata.add(context.metadata());
        });

    assertThat(visitedAdminIds).containsExactlyInAnyOrder(200L, 300L);
    assertThat(visitedViews)
        .containsExactlyInAnyOrder(
            AdminPanelSessionManager.AdminPanelView.MAIN,
            AdminPanelSessionManager.AdminPanelView.PRODUCT_DETAIL);
    assertThat(visitedMetadata)
        .anySatisfy(metadata -> assertThat(metadata).isEmpty())
        .anySatisfy(
            metadata ->
                assertThat(metadata).containsEntry("productId", 42L).containsEntry("page", 3));
    assertThat(sessionManager.getSessionContext(999L, 400L)).isPresent();
  }

  @Test
  @DisplayName("清除或失效的 session 應在遍歷時被移除")
  void updatePanelsByGuildShouldRemoveClearedSessions() {
    InteractionHook hook = mock(InteractionHook.class);

    sessionManager.registerSession(100L, 200L, hook);
    backingManager.clearSession(
        AdminPanelSessionManager.AdminPanelSessionType.ADMIN_PANEL, 100L, 200L);

    AtomicInteger invocations = new AtomicInteger();
    sessionManager.updatePanelsByGuild(
        100L,
        context -> {
          invocations.incrementAndGet();
        });

    assertThat(invocations.get()).isZero();
    assertThat(sessionManager.hasValidSession(100L, 200L)).isFalse();
    assertThat(sessionManager.getSessionContext(100L, 200L)).isEmpty();
  }

  @Test
  @DisplayName("consumer 失敗時應移除該 session")
  void updatePanelsByGuildShouldRemoveSessionWhenConsumerThrows() {
    InteractionHook hook = mock(InteractionHook.class);

    sessionManager.registerSession(100L, 200L, hook);

    sessionManager.updatePanelsByGuild(
        100L,
        context -> {
          throw new IllegalStateException("boom");
        });

    assertThat(sessionManager.hasValidSession(100L, 200L)).isFalse();
    assertThat(sessionManager.getSessionContext(100L, 200L)).isEmpty();
  }

  @Test
  @DisplayName("clearExpiredSessions 應同步清理 backing store 與本地索引")
  void clearExpiredSessionsShouldRemoveLocalStateWhenBackingStoreIsGone() {
    InteractionHook hook = mock(InteractionHook.class);

    sessionManager.registerSession(100L, 200L, hook);
    backingManager.clearSession(
        AdminPanelSessionManager.AdminPanelSessionType.ADMIN_PANEL, 100L, 200L);

    sessionManager.clearExpiredSessions();

    assertThat(sessionManager.hasValidSession(100L, 200L)).isFalse();
    assertThat(sessionManager.getSessionContext(100L, 200L)).isEmpty();
  }

  private static final class FakeDiscordSessionManager
      implements DiscordSessionManager<AdminPanelSessionManager.AdminPanelSessionType> {

    private final ConcurrentHashMap<String, Session<AdminPanelSessionManager.AdminPanelSessionType>>
        sessions = new ConcurrentHashMap<>();

    @Override
    public void registerSession(
        AdminPanelSessionManager.AdminPanelSessionType type,
        long guildId,
        long userId,
        InteractionHook hook,
        Map<String, Object> metadata) {
      sessions.put(
          key(type, guildId, userId),
          new Session<>(
              type,
              hook,
              Instant.now(),
              DEFAULT_TTL_SECONDS,
              metadata == null ? Map.of() : Map.copyOf(metadata)));
    }

    @Override
    public Optional<Session<AdminPanelSessionManager.AdminPanelSessionType>> getSession(
        AdminPanelSessionManager.AdminPanelSessionType type, long guildId, long userId) {
      String key = key(type, guildId, userId);
      Session<AdminPanelSessionManager.AdminPanelSessionType> session = sessions.get(key);
      if (session == null) {
        return Optional.empty();
      }
      if (session.isExpired()) {
        sessions.remove(key);
        return Optional.empty();
      }
      return Optional.of(session);
    }

    @Override
    public void clearSession(
        AdminPanelSessionManager.AdminPanelSessionType type, long guildId, long userId) {
      sessions.remove(key(type, guildId, userId));
    }

    @Override
    public void clearExpiredSessions() {
      sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static String key(
        AdminPanelSessionManager.AdminPanelSessionType type, long guildId, long userId) {
      return type.name() + ":" + guildId + ":" + userId;
    }
  }
}
