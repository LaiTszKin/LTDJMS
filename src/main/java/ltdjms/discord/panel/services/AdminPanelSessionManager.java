package ltdjms.discord.panel.services;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.discord.domain.DiscordSessionManager;
import ltdjms.discord.discord.domain.SessionType;
import ltdjms.discord.discord.services.InteractionSessionManager;
import net.dv8tion.jda.api.interactions.InteractionHook;

/**
 * Admin Panel Session 管理器。
 *
 * <p>此類別管理管理員面板的 Session，除了保留 InteractionHook 以便在 15 分鐘有效期內 進行編輯，也保留可枚舉的 view metadata，讓 guild-wide
 * refresh 能根據目前畫面內容做真實更新。
 */
public class AdminPanelSessionManager {

  private static final Logger LOG = LoggerFactory.getLogger(AdminPanelSessionManager.class);

  /** Admin Panel 使用的 Session 類型 */
  public enum AdminPanelSessionType implements SessionType {
    ADMIN_PANEL
  }

  /** 目前可由 guild-wide refresh 路徑處理的 admin panel view。 */
  public enum AdminPanelView {
    MAIN,
    PRODUCT_LIST,
    PRODUCT_DETAIL,
    PRODUCT_CODE_LIST
  }

  /** Admin panel traversal 時使用的完整 session context。 */
  public record AdminPanelSessionContext(
      long guildId,
      long adminId,
      InteractionHook hook,
      AdminPanelView currentView,
      Map<String, Object> metadata) {}

  private static final String PRODUCT_ID_KEY = "productId";
  private static final String PAGE_KEY = "page";

  private final DiscordSessionManager<AdminPanelSessionType> sessionManager;
  private final ConcurrentHashMap<String, AdminPanelSessionState> sessionStates;

  /** 建立一個新的 AdminPanelSessionManager */
  public AdminPanelSessionManager() {
    this(new InteractionSessionManager<>());
  }

  /**
   * 建立一個新的 AdminPanelSessionManager（允許注入 SessionManager 用於測試）
   *
   * @param sessionManager 自訂的 SessionManager
   */
  AdminPanelSessionManager(DiscordSessionManager<AdminPanelSessionType> sessionManager) {
    this.sessionManager = sessionManager;
    this.sessionStates = new ConcurrentHashMap<>();
  }

  /**
   * 註冊一個管理員面板 Session。
   *
   * @param guildId Discord Guild ID
   * @param adminId 管理員使用者 ID
   * @param hook InteractionHook
   */
  public void registerSession(long guildId, long adminId, InteractionHook hook) {
    sessionManager.registerSession(
        AdminPanelSessionType.ADMIN_PANEL, guildId, adminId, hook, Map.of());
    sessionStates.put(
        sessionKey(guildId, adminId), new AdminPanelSessionState(AdminPanelView.MAIN, Map.of()));
    LOG.debug("已註冊管理員面板 Session：guildId={}, adminId={}", guildId, adminId);
  }

  /**
   * 更新管理員面板（如果 Session 存在且有效）。
   *
   * @param guildId Discord Guild ID
   * @param adminId 管理員使用者 ID
   * @param consumer 對 InteractionHook 執行的操作
   */
  public void updatePanel(long guildId, long adminId, Consumer<InteractionHook> consumer) {
    Optional<AdminPanelSessionContext> contextOpt = getSessionContext(guildId, adminId);
    if (contextOpt.isEmpty()) {
      LOG.debug("管理員面板 Session 不存在或已過期：guildId={}, adminId={}", guildId, adminId);
      return;
    }

    AdminPanelSessionContext context = contextOpt.get();
    try {
      consumer.accept(context.hook());
    } catch (Exception e) {
      LOG.warn("更新管理員面板失敗，移除 Session：guildId={}, adminId={}", guildId, adminId, e);
      clearSession(guildId, adminId);
    }
  }

  /**
   * 更新 Session 目前所在的 view 與對應 metadata。
   *
   * <p>此方法用來記錄 admin panel 目前正在顯示的畫面，以便 guild-wide refresh 能依 view 重新建構正確的 embed / components。
   */
  public void updateSessionView(
      long guildId, long adminId, AdminPanelView currentView, Map<String, Object> metadata) {
    String key = sessionKey(guildId, adminId);
    Optional<DiscordSessionManager.Session<AdminPanelSessionType>> sessionOpt =
        sessionManager.getSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId);
    if (sessionOpt.isEmpty()) {
      sessionStates.remove(key);
      LOG.debug("略過更新已失效的管理員面板 Session：guildId={}, adminId={}", guildId, adminId);
      return;
    }

    sessionStates.put(key, new AdminPanelSessionState(currentView, safeMetadata(metadata)));
  }

  /** 取得指定 Session 的快照。 */
  public Optional<AdminPanelSessionContext> getSessionContext(long guildId, long adminId) {
    Optional<DiscordSessionManager.Session<AdminPanelSessionType>> sessionOpt =
        sessionManager.getSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId);
    if (sessionOpt.isEmpty()) {
      sessionStates.remove(sessionKey(guildId, adminId));
      return Optional.empty();
    }

    AdminPanelSessionState state =
        sessionStates.getOrDefault(
            sessionKey(guildId, adminId),
            new AdminPanelSessionState(AdminPanelView.MAIN, Map.of()));
    return Optional.of(
        new AdminPanelSessionContext(
            guildId, adminId, sessionOpt.get().hook(), state.currentView(), state.metadata()));
  }

  /**
   * 清除指定的 Session。
   *
   * <p>當管理員關閉面板時應呼叫此方法。
   *
   * @param guildId Discord Guild ID
   * @param adminId 管理員使用者 ID
   */
  public void clearSession(long guildId, long adminId) {
    sessionManager.clearSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId);
    sessionStates.remove(sessionKey(guildId, adminId));
    LOG.debug("已清除管理員面板 Session：guildId={}, adminId={}", guildId, adminId);
  }

  /**
   * 更新指定 Guild 的所有管理員面板。
   *
   * <p>呼叫端可依 {@link AdminPanelSessionContext#currentView()} 來挑選是否刷新該 session。 過期或無效的 hook 會在遍歷時被移除。
   */
  public void updatePanelsByGuild(long guildId, Consumer<AdminPanelSessionContext> consumer) {
    String guildPrefix = guildId + ":";
    sessionStates
        .entrySet()
        .removeIf(
            entry -> {
              String key = entry.getKey();
              if (!key.startsWith(guildPrefix)) {
                return false;
              }

              long adminId;
              try {
                adminId = Long.parseLong(key.substring(guildPrefix.length()));
              } catch (NumberFormatException e) {
                LOG.warn("略過格式錯誤的管理員面板 Session key：{}", key, e);
                return true;
              }

              Optional<DiscordSessionManager.Session<AdminPanelSessionType>> sessionOpt =
                  sessionManager.getSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId);
              if (sessionOpt.isEmpty()) {
                return true;
              }

              AdminPanelSessionState state = entry.getValue();
              try {
                consumer.accept(
                    new AdminPanelSessionContext(
                        guildId,
                        adminId,
                        sessionOpt.get().hook(),
                        state.currentView(),
                        state.metadata()));
                return false;
              } catch (Exception e) {
                LOG.warn("更新管理員面板失敗，移除 Session：guildId={}, adminId={}", guildId, adminId, e);
                sessionManager.clearSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId);
                return true;
              }
            });
  }

  /**
   * 清除所有過期的 Session。
   *
   * <p>建議定期呼叫此方法以釋放記憶體。
   */
  public void clearExpiredSessions() {
    sessionManager.clearExpiredSessions();
    sessionStates
        .entrySet()
        .removeIf(
            entry -> {
              long[] ids = parseSessionKey(entry.getKey());
              return ids == null
                  || sessionManager
                      .getSession(AdminPanelSessionType.ADMIN_PANEL, ids[0], ids[1])
                      .isEmpty();
            });
    LOG.debug("已清除所有過期的管理員面板 Session");
  }

  /**
   * 檢查指定 Session 是否存在且有效。
   *
   * @param guildId Discord Guild ID
   * @param adminId 管理員使用者 ID
   * @return true 如果 Session 存在且未過期
   */
  public boolean hasValidSession(long guildId, long adminId) {
    return sessionManager
        .getSession(AdminPanelSessionType.ADMIN_PANEL, guildId, adminId)
        .isPresent();
  }

  private static String sessionKey(long guildId, long adminId) {
    return guildId + ":" + adminId;
  }

  private static Map<String, Object> safeMetadata(Map<String, Object> metadata) {
    return metadata == null || metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
  }

  private static long[] parseSessionKey(String key) {
    int separatorIndex = key.indexOf(':');
    if (separatorIndex < 0 || separatorIndex == key.length() - 1) {
      return null;
    }

    try {
      long guildId = Long.parseLong(key.substring(0, separatorIndex));
      long adminId = Long.parseLong(key.substring(separatorIndex + 1));
      return new long[] {guildId, adminId};
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private record AdminPanelSessionState(AdminPanelView currentView, Map<String, Object> metadata) {}
}
