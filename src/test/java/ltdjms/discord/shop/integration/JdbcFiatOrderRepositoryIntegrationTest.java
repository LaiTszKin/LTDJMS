package ltdjms.discord.shop.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ltdjms.discord.shared.DatabaseMigrationRunner;
import ltdjms.discord.shop.domain.FiatOrder;
import ltdjms.discord.shop.persistence.JdbcFiatOrderRepository;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("JdbcFiatOrderRepository 整合測試")
class JdbcFiatOrderRepositoryIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("fiat_order_expiry_test")
          .withUsername("test")
          .withPassword("test");

  private HikariDataSource dataSource;
  private JdbcFiatOrderRepository repository;

  @BeforeEach
  void setUp() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(postgres.getJdbcUrl());
    config.setUsername(postgres.getUsername());
    config.setPassword(postgres.getPassword());
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    dataSource = new HikariDataSource(config);
    DatabaseMigrationRunner.forDefaultMigrations().migrate(dataSource);
    repository = new JdbcFiatOrderRepository(dataSource);
    truncateTable(dataSource);
  }

  @AfterEach
  void tearDown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  @Test
  @Timeout(60)
  @DisplayName("應持久化到期時間並保留 pending 狀態")
  void shouldPersistExpireAtForPendingOrder() {
    Instant expireAt = Instant.parse("2026-04-11T12:00:00Z");
    FiatOrder saved =
        repository.save(
            FiatOrder.createPending(
                1L, 2L, 3L, "法幣商品", "FD260411000001", "ABC123456789", 1200L, expireAt));

    assertThat(saved.expireAt()).isEqualTo(expireAt);
    FiatOrder reloaded = repository.findByOrderNumber("FD260411000001").orElseThrow();
    assertThat(reloaded.status()).isEqualTo(FiatOrder.Status.PENDING_PAYMENT);
    assertThat(reloaded.expireAt()).isEqualTo(expireAt);
    assertThat(reloaded.expiredAt()).isNull();
    assertThat(reloaded.terminalReason()).isNull();
  }

  @Test
  @Timeout(60)
  @DisplayName("reconciliation selection 應排除已逾期訂單")
  void shouldExcludeExpiredOrdersFromReconciliationSelection() {
    Instant now = Instant.parse("2026-04-11T12:00:00Z");
    FiatOrder expired =
        repository.save(
            FiatOrder.createPending(
                1L, 2L, 3L, "過期商品", "FD260411000002", "ABC123456780", 1200L, now.minusSeconds(1)));
    FiatOrder live =
        repository.save(
            FiatOrder.createPending(
                1L, 2L, 4L, "有效商品", "FD260411000003", "ABC123456781", 1200L, now.plusSeconds(600)));

    assertThat(repository.findOrdersPendingExpiry(now, 20))
        .extracting(FiatOrder::orderNumber)
        .containsExactly(expired.orderNumber());
    assertThat(
            repository.findOrdersPendingReconciliation(now, now.minusSeconds(7 * 24 * 60 * 60), 20))
        .extracting(FiatOrder::orderNumber)
        .containsExactly(live.orderNumber());
  }

  @Test
  @Timeout(60)
  @DisplayName("逾期轉態後不應再被標記為已付款")
  void shouldBlockPaidTransitionAfterExpiry() {
    Instant now = Instant.parse("2026-04-11T12:00:00Z");
    FiatOrder pending =
        repository.save(
            FiatOrder.createPending(
                1L, 2L, 3L, "過期商品", "FD260411000004", "ABC123456782", 1200L, now.minusSeconds(60)));

    FiatOrder expired =
        repository.markExpiredIfPending(pending.orderNumber(), now, "EXPIRED").orElseThrow();
    assertThat(expired.status()).isEqualTo(FiatOrder.Status.EXPIRED);
    assertThat(expired.expiredAt()).isEqualTo(now);
    assertThat(expired.terminalReason()).isEqualTo("EXPIRED");
    assertThat(
            repository.markPaidIfPending(
                pending.orderNumber(), "1", "付款成功", "{}", now.plusSeconds(1)))
        .isEmpty();
  }

  private void truncateTable(DataSource ds) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE TABLE fiat_order CASCADE");
    } catch (Exception e) {
      throw new RuntimeException("Failed to clean fiat_order table", e);
    }
  }
}
