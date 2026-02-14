package ltdjms.discord.dispatch.domain;

import java.util.Set;

/** 派單系統售後人員設定的持久化介面。 */
public interface DispatchAfterSalesStaffRepository {

  /** 查詢 guild 設定的所有售後人員 userId。 */
  Set<Long> findStaffUserIds(long guildId);

  /**
   * 新增售後人員。
   *
   * @return true 表示新增成功；false 表示已存在
   */
  boolean addStaff(long guildId, long userId);

  /**
   * 移除售後人員。
   *
   * @return true 表示移除成功；false 表示不存在
   */
  boolean removeStaff(long guildId, long userId);
}
