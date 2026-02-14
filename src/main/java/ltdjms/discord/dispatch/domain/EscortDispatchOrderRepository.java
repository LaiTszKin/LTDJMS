package ltdjms.discord.dispatch.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 派單護航訂單的持久化介面。 */
public interface EscortDispatchOrderRepository {

  /** 儲存新訂單並回傳帶有資料庫主鍵的實體。 */
  EscortDispatchOrder save(EscortDispatchOrder order);

  /** 更新既有訂單並回傳最新狀態。 */
  EscortDispatchOrder update(EscortDispatchOrder order);

  /** 依訂單編號查詢。 */
  Optional<EscortDispatchOrder> findByOrderNumber(String orderNumber);

  /** 取得 guild 最近建立的訂單（依建立時間遞減）。 */
  List<EscortDispatchOrder> findRecentByGuildId(long guildId, int limit);

  /**
   * 原子接手售後案件。
   *
   * <p>僅在訂單狀態為 AFTER_SALES_REQUESTED 且尚未被接手時成功。
   */
  Optional<EscortDispatchOrder> claimAfterSales(
      String orderNumber, long assigneeUserId, Instant assignedAt);

  /**
   * 原子售後結案。
   *
   * <p>僅接手者本人且狀態為 AFTER_SALES_IN_PROGRESS 時成功。
   */
  Optional<EscortDispatchOrder> closeAfterSales(
      String orderNumber, long assigneeUserId, Instant closedAt);

  /** 檢查訂單編號是否已存在。 */
  boolean existsByOrderNumber(String orderNumber);
}
