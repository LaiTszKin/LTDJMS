package ltdjms.discord.dispatch.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ltdjms.discord.dispatch.domain.EscortDispatchOrder;
import ltdjms.discord.dispatch.persistence.JdbcEscortDispatchOrderRepository;
import ltdjms.discord.dispatch.services.EscortDispatchHandoffService;
import ltdjms.discord.product.domain.Product;
import ltdjms.discord.product.persistence.JdbcProductRepository;
import ltdjms.discord.product.services.ProductService;
import ltdjms.discord.redemption.persistence.JdbcRedemptionCodeRepository;
import ltdjms.discord.shared.DatabaseMigrationRunner;
import ltdjms.discord.shared.Result;
import ltdjms.discord.shared.events.DomainEventPublisher;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("EscortDispatchHandoffService 整合測試")
class EscortDispatchHandoffServiceIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("escort_handoff_test")
          .withUsername("test")
          .withPassword("test");

  private HikariDataSource dataSource;
  private ProductService productService;
  private JdbcEscortDispatchOrderRepository dispatchRepository;
  private EscortDispatchHandoffService handoffService;

  @BeforeEach
  void setUp() {
    dataSource = createDataSource();
    DatabaseMigrationRunner.forDefaultMigrations().migrate(dataSource);
    truncateTables(dataSource);

    productService =
        new ProductService(
            new JdbcProductRepository(dataSource),
            new JdbcRedemptionCodeRepository(dataSource),
            new DomainEventPublisher());
    dispatchRepository = new JdbcEscortDispatchOrderRepository(dataSource);
    handoffService = new EscortDispatchHandoffService(dispatchRepository);
  }

  @AfterEach
  void tearDown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  @Test
  @DisplayName("貨幣購買交接應保存來源快照，且刪除商品後仍可查回")
  void shouldPersistCurrencyHandoffSnapshotAndSurviveProductDeletion() {
    Product product = createAutoEscortProduct(123456789L, "貨幣護航商品", 100L, null, "CONF_HOURLY_1H");
    Result<EscortDispatchOrder, ltdjms.discord.shared.DomainError> result =
        handoffService.handoffFromCurrencyPurchase(
            product.guildId(), 987654321L, product, "interaction-1234567890");

    assertThat(result.isOk()).isTrue();
    EscortDispatchOrder order = result.getValue();
    assertThat(order.sourceType()).isEqualTo(EscortDispatchOrder.SourceType.CURRENCY_PURCHASE);
    assertThat(order.sourceReference()).isEqualTo("interaction-1234567890");
    assertThat(order.sourceProductId()).isEqualTo(product.id());
    assertThat(order.sourceProductName()).isEqualTo(product.name());
    assertThat(order.sourceCurrencyPrice()).isEqualTo(product.currencyPrice());
    assertThat(order.sourceFiatPriceTwd()).isNull();
    assertThat(order.sourceEscortOptionCode()).isEqualTo(product.escortOptionCode());
    assertThat(order.customerUserId()).isEqualTo(987654321L);
    assertThat(order.escortUserId()).isZero();

    assertThat(productService.deleteProduct(product.id()).isOk()).isTrue();

    EscortDispatchOrder persisted =
        dispatchRepository.findByOrderNumber(order.orderNumber()).orElseThrow();
    assertThat(persisted.sourceProductName()).isEqualTo("貨幣護航商品");
    assertThat(persisted.sourceEscortOptionCode()).isEqualTo("CONF_HOURLY_1H");
  }

  @Test
  @DisplayName("重複來源參考應回傳同一筆護航交接")
  void shouldReturnExistingOrderForDuplicateSourceReference() {
    Product product = createAutoEscortProduct(123456789L, "重播護航商品", 100L, null, "CONF_HOURLY_1H");

    Result<EscortDispatchOrder, ltdjms.discord.shared.DomainError> first =
        handoffService.handoffFromCurrencyPurchase(
            product.guildId(), 987654321L, product, "interaction-duplicate");
    Result<EscortDispatchOrder, ltdjms.discord.shared.DomainError> second =
        handoffService.handoffFromCurrencyPurchase(
            product.guildId(), 987654321L, product, "interaction-duplicate");

    assertThat(first.isOk()).isTrue();
    assertThat(second.isOk()).isTrue();
    assertThat(second.getValue().orderNumber()).isEqualTo(first.getValue().orderNumber());
    assertThat(dispatchRepository.findRecentByGuildId(product.guildId(), 10)).hasSize(1);
  }

  @Test
  @DisplayName("法幣付款交接應保存法幣價格快照")
  void shouldPersistFiatHandoffSnapshot() {
    Product product = createAutoEscortProduct(123456789L, "法幣護航商品", null, 1200L, "CONF_DAM_300W");

    Result<EscortDispatchOrder, ltdjms.discord.shared.DomainError> result =
        handoffService.handoffFromFiatPayment(
            product.guildId(), 987654321L, product, "FD260411000001");

    assertThat(result.isOk()).isTrue();
    EscortDispatchOrder order = result.getValue();
    assertThat(order.sourceType()).isEqualTo(EscortDispatchOrder.SourceType.FIAT_PAYMENT);
    assertThat(order.sourceReference()).isEqualTo("FD260411000001");
    assertThat(order.sourceCurrencyPrice()).isNull();
    assertThat(order.sourceFiatPriceTwd()).isEqualTo(1200L);
    assertThat(order.sourceEscortOptionCode()).isEqualTo("CONF_DAM_300W");
  }

  private HikariDataSource createDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(postgres.getJdbcUrl());
    config.setUsername(postgres.getUsername());
    config.setPassword(postgres.getPassword());
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setPoolName("EscortHandoffIntegrationPool");
    return new HikariDataSource(config);
  }

  private void truncateTables(DataSource ds) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE TABLE escort_dispatch_order CASCADE");
      stmt.execute("TRUNCATE TABLE product CASCADE");
      stmt.execute("TRUNCATE TABLE redemption_code CASCADE");
    } catch (Exception e) {
      throw new RuntimeException("Failed to clean test tables", e);
    }
  }

  private Product createAutoEscortProduct(
      long guildId, String name, Long currencyPrice, Long fiatPriceTwd, String escortOptionCode) {
    Result<Product, ltdjms.discord.shared.DomainError> result =
        productService.createProduct(
            guildId, name, "desc", null, null, currencyPrice, fiatPriceTwd, true, escortOptionCode);
    assertThat(result.isOk()).isTrue();
    return result.getValue();
  }
}
