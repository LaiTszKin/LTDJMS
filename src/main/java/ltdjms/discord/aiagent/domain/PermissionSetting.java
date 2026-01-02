package ltdjms.discord.aiagent.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 權限設定資料傳輸物件。
 *
 * <p>用於 LangChain4J 工具方法，替代 {@code Map<String, Object>} 以避免嵌套泛型問題。
 *
 * @param roleId 角色 ID
 * @param allowSet 允許的權限集合（可選）
 * @param denySet 拒絕的權限集合（可選）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionSetting(
    @JsonProperty("roleId") long roleId,
    @JsonProperty(value = "allowSet", required = false) java.util.Set<PermissionEnum> allowSet,
    @JsonProperty(value = "denySet", required = false) java.util.Set<PermissionEnum> denySet) {

  /**
   * JSON 反序列化工廠，向後相容舊版的 permissionSet 寫法。
   *
   * @param roleId 角色 ID
   * @param allowSet 允許權限集合
   * @param denySet 拒絕權限集合
   * @param permissionSet 舊版權限集合字串（如 admin_only/private）
   * @return 轉換後的 PermissionSetting
   */
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public static PermissionSetting fromJson(
      @JsonProperty("roleId") long roleId,
      @JsonProperty("allowSet") java.util.Set<PermissionEnum> allowSet,
      @JsonProperty("denySet") java.util.Set<PermissionEnum> denySet,
      @JsonProperty("permissionSet") String permissionSet) {

    return new PermissionSetting(roleId, mergeAllowSet(allowSet, permissionSet), denySet);
  }

  /** 優先使用 allowSet；若為空則嘗試從 permissionSet 字串映射預設權限組合。 */
  private static java.util.Set<PermissionEnum> mergeAllowSet(
      java.util.Set<PermissionEnum> allowSet, String permissionSet) {

    if (allowSet != null && !allowSet.isEmpty()) {
      return allowSet;
    }

    if (permissionSet == null || permissionSet.isBlank()) {
      return allowSet;
    }

    return switch (permissionSet.trim().toLowerCase()) {
      case "admin_only", "admin-only", "admins_only" ->
          java.util.Set.of(
              PermissionEnum.ADMINISTRATOR,
              PermissionEnum.VIEW_CHANNEL,
              PermissionEnum.MESSAGE_SEND);
      case "private", "private_only" ->
          java.util.Set.of(PermissionEnum.VIEW_CHANNEL, PermissionEnum.MESSAGE_SEND);
      case "read_only", "readonly" -> java.util.Set.of(PermissionEnum.VIEW_CHANNEL);
      case "full", "all" ->
          java.util.Set.of(
              PermissionEnum.ADMINISTRATOR,
              PermissionEnum.MANAGE_CHANNELS,
              PermissionEnum.MANAGE_ROLES,
              PermissionEnum.MANAGE_SERVER,
              PermissionEnum.VIEW_CHANNEL,
              PermissionEnum.MESSAGE_SEND,
              PermissionEnum.MESSAGE_HISTORY,
              PermissionEnum.VOICE_CONNECT,
              PermissionEnum.VOICE_SPEAK,
              PermissionEnum.PRIORITY_SPEAKER);
      default -> allowSet;
    };
  }

  /**
   * 權限枚舉（字串格式）。
   *
   * <p>對應 Discord 的 {@link net.dv8tion.jda.api.Permission}。
   */
  public enum PermissionEnum {
    /** 管理員 */
    ADMINISTRATOR,
    /** 管理頻道 */
    MANAGE_CHANNELS,
    /** 管理角色 */
    MANAGE_ROLES,
    /** 管理伺服器 */
    MANAGE_SERVER,
    /** 查看頻道 */
    VIEW_CHANNEL,
    /** 發送訊息 */
    MESSAGE_SEND,
    /** 讀取訊息歷史 */
    MESSAGE_HISTORY,
    /** 連結頻道 */
    VOICE_CONNECT,
    /** 說話 */
    VOICE_SPEAK,
    /** 優先權 */
    PRIORITY_SPEAKER
  }

  /**
   * 建構空的權限設定實例。
   *
   * <p>用於 JSON 反序列化。
   */
  public PermissionSetting {
    if (allowSet == null) {
      allowSet = java.util.Set.of();
    }
    if (denySet == null) {
      denySet = java.util.Set.of();
    }
  }
}
