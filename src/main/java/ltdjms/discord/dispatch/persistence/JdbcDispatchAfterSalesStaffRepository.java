package ltdjms.discord.dispatch.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ltdjms.discord.dispatch.domain.DispatchAfterSalesStaffRepository;

/** JDBC 實作：派單系統售後人員設定。 */
public class JdbcDispatchAfterSalesStaffRepository implements DispatchAfterSalesStaffRepository {

  private static final Logger LOG =
      LoggerFactory.getLogger(JdbcDispatchAfterSalesStaffRepository.class);

  private final DataSource dataSource;

  public JdbcDispatchAfterSalesStaffRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Set<Long> findStaffUserIds(long guildId) {
    String sql =
        "SELECT user_id FROM dispatch_after_sales_staff WHERE guild_id = ? ORDER BY created_at ASC";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, guildId);

      Set<Long> staffUserIds = new HashSet<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          staffUserIds.add(rs.getLong("user_id"));
        }
      }
      return staffUserIds;
    } catch (SQLException e) {
      LOG.error("Failed to find dispatch after-sales staff: guildId={}", guildId, e);
      throw new RepositoryException("Failed to find dispatch after-sales staff", e);
    }
  }

  @Override
  public boolean addStaff(long guildId, long userId) {
    String sql =
        "INSERT INTO dispatch_after_sales_staff (guild_id, user_id) VALUES (?, ?) ON CONFLICT DO"
            + " NOTHING";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, guildId);
      stmt.setLong(2, userId);

      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      LOG.error(
          "Failed to add dispatch after-sales staff: guildId={}, userId={}", guildId, userId, e);
      throw new RepositoryException("Failed to add dispatch after-sales staff", e);
    }
  }

  @Override
  public boolean removeStaff(long guildId, long userId) {
    String sql = "DELETE FROM dispatch_after_sales_staff WHERE guild_id = ? AND user_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, guildId);
      stmt.setLong(2, userId);

      return stmt.executeUpdate() > 0;
    } catch (SQLException e) {
      LOG.error(
          "Failed to remove dispatch after-sales staff: guildId={}, userId={}", guildId, userId, e);
      throw new RepositoryException("Failed to remove dispatch after-sales staff", e);
    }
  }
}
