package ltdjms.discord.dispatch.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.domain.EscortDispatchOrderRepository;

/** JDBC 實作：護航派單訂單儲存。 */
public class JdbcEscortDispatchOrderRepository implements EscortDispatchOrderRepository {

  private static final Logger LOG =
      LoggerFactory.getLogger(JdbcEscortDispatchOrderRepository.class);

  private static final String SELECT_COLUMNS =
      "id, order_number, guild_id, assigned_by_user_id, escort_user_id, customer_user_id,"
          + " status, created_at, confirmed_at, completion_requested_at, completed_at,"
          + " after_sales_requested_at, after_sales_assignee_user_id, after_sales_assigned_at,"
          + " after_sales_closed_at, updated_at, source_type, source_reference,"
          + " source_product_id, source_product_name, source_currency_price, source_fiat_price_twd,"
          + " source_escort_option_code";

  private final DataSource dataSource;

  public JdbcEscortDispatchOrderRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public EscortDispatchOrder save(EscortDispatchOrder order) {
    String sql =
        "INSERT INTO escort_dispatch_order (order_number, guild_id, assigned_by_user_id,"
            + " escort_user_id, customer_user_id, status, created_at, confirmed_at,"
            + " completion_requested_at, completed_at, after_sales_requested_at,"
            + " after_sales_assignee_user_id, after_sales_assigned_at, after_sales_closed_at,"
            + " updated_at, source_type, source_reference, source_product_id, source_product_name,"
            + " source_currency_price, source_fiat_price_twd, source_escort_option_code)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            + " RETURNING id";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, order.orderNumber());
      stmt.setLong(2, order.guildId());
      stmt.setLong(3, order.assignedByUserId());
      stmt.setLong(4, order.escortUserId());
      stmt.setLong(5, order.customerUserId());
      stmt.setString(6, order.status().name());
      stmt.setTimestamp(7, Timestamp.from(order.createdAt()));
      setNullableTimestamp(stmt, 8, order.confirmedAt());
      setNullableTimestamp(stmt, 9, order.completionRequestedAt());
      setNullableTimestamp(stmt, 10, order.completedAt());
      setNullableTimestamp(stmt, 11, order.afterSalesRequestedAt());
      setNullableLong(stmt, 12, order.afterSalesAssigneeUserId());
      setNullableTimestamp(stmt, 13, order.afterSalesAssignedAt());
      setNullableTimestamp(stmt, 14, order.afterSalesClosedAt());
      stmt.setTimestamp(15, Timestamp.from(order.updatedAt()));
      stmt.setString(16, order.sourceType().name());
      setNullableString(stmt, 17, order.sourceReference());
      setNullableLong(stmt, 18, order.sourceProductId());
      setNullableString(stmt, 19, order.sourceProductName());
      setNullableLong(stmt, 20, order.sourceCurrencyPrice());
      setNullableLong(stmt, 21, order.sourceFiatPriceTwd());
      setNullableString(stmt, 22, order.sourceEscortOptionCode());

      try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          throw new RepositoryException("Failed to save escort dispatch order");
        }

        long id = rs.getLong("id");
        EscortDispatchOrder saved =
            new EscortDispatchOrder(
                id,
                order.orderNumber(),
                order.guildId(),
                order.assignedByUserId(),
                order.escortUserId(),
                order.customerUserId(),
                order.createdAt(),
                order.confirmedAt(),
                order.completionRequestedAt(),
                order.completedAt(),
                order.afterSalesRequestedAt(),
                order.afterSalesAssigneeUserId(),
                order.afterSalesAssignedAt(),
                order.afterSalesClosedAt(),
                order.updatedAt(),
                order.sourceType(),
                order.sourceReference(),
                order.sourceProductId(),
                order.sourceProductName(),
                order.sourceCurrencyPrice(),
                order.sourceFiatPriceTwd(),
                order.sourceEscortOptionCode(),
                order.status());

        LOG.debug("Saved escort dispatch order: id={}, orderNumber={}", id, order.orderNumber());
        return saved;
      }
    } catch (SQLException e) {
      LOG.error("Failed to save escort dispatch order: orderNumber={}", order.orderNumber(), e);
      throw new RepositoryException("Failed to save escort dispatch order", e);
    }
  }

  @Override
  public EscortDispatchOrder update(EscortDispatchOrder order) {
    if (order.id() == null) {
      throw new IllegalArgumentException("Cannot update order without ID");
    }

    String sql =
        "UPDATE escort_dispatch_order SET status = ?, confirmed_at = ?, completion_requested_at ="
            + " ?, completed_at = ?, after_sales_requested_at = ?, after_sales_assignee_user_id ="
            + " ?, after_sales_assigned_at = ?, after_sales_closed_at = ?, updated_at = ?"
            + " WHERE id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, order.status().name());
      setNullableTimestamp(stmt, 2, order.confirmedAt());
      setNullableTimestamp(stmt, 3, order.completionRequestedAt());
      setNullableTimestamp(stmt, 4, order.completedAt());
      setNullableTimestamp(stmt, 5, order.afterSalesRequestedAt());
      setNullableLong(stmt, 6, order.afterSalesAssigneeUserId());
      setNullableTimestamp(stmt, 7, order.afterSalesAssignedAt());
      setNullableTimestamp(stmt, 8, order.afterSalesClosedAt());
      stmt.setTimestamp(9, Timestamp.from(order.updatedAt()));
      stmt.setLong(10, order.id());

      int affected = stmt.executeUpdate();
      if (affected == 0) {
        throw new RepositoryException("Escort dispatch order not found, id=" + order.id());
      }

      LOG.debug("Updated escort dispatch order: id={}, status={}", order.id(), order.status());
      return order;
    } catch (SQLException e) {
      LOG.error("Failed to update escort dispatch order: id={}", order.id(), e);
      throw new RepositoryException("Failed to update escort dispatch order", e);
    }
  }

  @Override
  public Optional<EscortDispatchOrder> findByOrderNumber(String orderNumber) {
    String sql = "SELECT " + SELECT_COLUMNS + " FROM escort_dispatch_order WHERE order_number = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, orderNumber);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      LOG.error("Failed to find escort dispatch order: orderNumber={}", orderNumber, e);
      throw new RepositoryException("Failed to find escort dispatch order", e);
    }
  }

  @Override
  public Optional<EscortDispatchOrder> findBySourceIdentity(
      EscortDispatchOrder.SourceType sourceType, String sourceReference) {
    if (sourceType == null || sourceReference == null || sourceReference.isBlank()) {
      return Optional.empty();
    }

    String sql =
        "SELECT "
            + SELECT_COLUMNS
            + " FROM escort_dispatch_order"
            + " WHERE source_type = ? AND source_reference = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, sourceType.name());
      stmt.setString(2, sourceReference);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      LOG.error(
          "Failed to find escort dispatch order by source identity: sourceType={},"
              + " sourceReference={}",
          sourceType,
          sourceReference,
          e);
      throw new RepositoryException("Failed to find escort dispatch order by source identity", e);
    }
  }

  @Override
  public List<EscortDispatchOrder> findRecentByGuildId(long guildId, int limit) {
    String sql =
        "SELECT "
            + SELECT_COLUMNS
            + " FROM escort_dispatch_order WHERE guild_id = ?"
            + " ORDER BY created_at DESC LIMIT ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, guildId);
      stmt.setInt(2, limit);

      List<EscortDispatchOrder> orders = new ArrayList<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          orders.add(mapRow(rs));
        }
      }
      return orders;
    } catch (SQLException e) {
      LOG.error(
          "Failed to query recent escort dispatch orders: guildId={}, limit={}", guildId, limit, e);
      throw new RepositoryException("Failed to query recent escort dispatch orders", e);
    }
  }

  @Override
  public Optional<EscortDispatchOrder> claimAfterSales(
      String orderNumber, long assigneeUserId, Instant assignedAt) {
    String sql =
        "UPDATE escort_dispatch_order"
            + " SET status = ?, after_sales_assignee_user_id = ?, after_sales_assigned_at = ?,"
            + " updated_at = ?"
            + " WHERE order_number = ? AND status = ? AND after_sales_assignee_user_id IS NULL"
            + " RETURNING "
            + SELECT_COLUMNS;

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS.name());
      stmt.setLong(2, assigneeUserId);
      stmt.setTimestamp(3, Timestamp.from(assignedAt));
      stmt.setTimestamp(4, Timestamp.from(assignedAt));
      stmt.setString(5, orderNumber);
      stmt.setString(6, EscortDispatchOrder.Status.AFTER_SALES_REQUESTED.name());

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      LOG.error(
          "Failed to claim after-sales case: orderNumber={}, assigneeUserId={}",
          orderNumber,
          assigneeUserId,
          e);
      throw new RepositoryException("Failed to claim after-sales case", e);
    }
  }

  @Override
  public Optional<EscortDispatchOrder> closeAfterSales(
      String orderNumber, long assigneeUserId, Instant closedAt) {
    String sql =
        "UPDATE escort_dispatch_order"
            + " SET status = ?, after_sales_closed_at = ?, updated_at = ?"
            + " WHERE order_number = ? AND status = ? AND after_sales_assignee_user_id = ?"
            + " RETURNING "
            + SELECT_COLUMNS;

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, EscortDispatchOrder.Status.AFTER_SALES_CLOSED.name());
      stmt.setTimestamp(2, Timestamp.from(closedAt));
      stmt.setTimestamp(3, Timestamp.from(closedAt));
      stmt.setString(4, orderNumber);
      stmt.setString(5, EscortDispatchOrder.Status.AFTER_SALES_IN_PROGRESS.name());
      stmt.setLong(6, assigneeUserId);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      LOG.error(
          "Failed to close after-sales case: orderNumber={}, assigneeUserId={}",
          orderNumber,
          assigneeUserId,
          e);
      throw new RepositoryException("Failed to close after-sales case", e);
    }
  }

  @Override
  public boolean existsByOrderNumber(String orderNumber) {
    String sql = "SELECT 1 FROM escort_dispatch_order WHERE order_number = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, orderNumber);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      LOG.error("Failed to check escort dispatch order existence: orderNumber={}", orderNumber, e);
      throw new RepositoryException("Failed to check escort dispatch order existence", e);
    }
  }

  private EscortDispatchOrder mapRow(ResultSet rs) throws SQLException {
    return new EscortDispatchOrder(
        rs.getLong("id"),
        rs.getString("order_number"),
        rs.getLong("guild_id"),
        rs.getLong("assigned_by_user_id"),
        rs.getLong("escort_user_id"),
        rs.getLong("customer_user_id"),
        getInstant(rs, "created_at"),
        getNullableInstant(rs, "confirmed_at"),
        getNullableInstant(rs, "completion_requested_at"),
        getNullableInstant(rs, "completed_at"),
        getNullableInstant(rs, "after_sales_requested_at"),
        getNullableLong(rs, "after_sales_assignee_user_id"),
        getNullableInstant(rs, "after_sales_assigned_at"),
        getNullableInstant(rs, "after_sales_closed_at"),
        getInstant(rs, "updated_at"),
        EscortDispatchOrder.SourceType.valueOf(rs.getString("source_type")),
        rs.getString("source_reference"),
        getNullableLong(rs, "source_product_id"),
        rs.getString("source_product_name"),
        getNullableLong(rs, "source_currency_price"),
        getNullableLong(rs, "source_fiat_price_twd"),
        rs.getString("source_escort_option_code"),
        EscortDispatchOrder.Status.valueOf(rs.getString("status")));
  }

  private Instant getInstant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    if (value == null) {
      throw new SQLException("Column " + column + " is null");
    }
    return value.toInstant();
  }

  private Instant getNullableInstant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value != null ? value.toInstant() : null;
  }

  private Long getNullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }

  private void setNullableTimestamp(PreparedStatement stmt, int index, Instant value)
      throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.TIMESTAMP);
      return;
    }
    stmt.setTimestamp(index, Timestamp.from(value));
  }

  private void setNullableLong(PreparedStatement stmt, int index, Long value) throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.BIGINT);
      return;
    }
    stmt.setLong(index, value);
  }

  private void setNullableString(PreparedStatement stmt, int index, String value)
      throws SQLException {
    if (value == null) {
      stmt.setNull(index, Types.VARCHAR);
      return;
    }
    stmt.setString(index, value);
  }
}
