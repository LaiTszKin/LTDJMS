package ltdjms.discord.aiagent.integration.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ltdjms.discord.aiagent.services.ToolExecutionContext;
import ltdjms.discord.aiagent.services.tools.LangChain4jGetChannelPermissionsTool;
import ltdjms.discord.shared.di.JDAProvider;

/**
 * 整合測試 {@link LangChain4jGetChannelPermissionsTool}。
 *
 * <p>測試範圍：
 *
 * <ul>
 *   <li>T024: LangChain4jGetChannelPermissionsTool 整合測試
 * </ul>
 *
 * <p>使用真實的 Discord API 進行測試（需要有效的測試環境）。
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("T024: LangChain4jGetChannelPermissionsTool 整合測試")
class LangChain4jGetChannelPermissionsToolIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("test_db")
          .withUsername("test")
          .withPassword("test");

  private LangChain4jGetChannelPermissionsTool tool;

  @BeforeAll
  static void setUpBeforeAll() {
    // 確保 PostgreSQL 容器已啟動
    POSTGRES.start();
  }

  @BeforeEach
  void setUp() {
    tool = new LangChain4jGetChannelPermissionsTool();

    // 注意：整合測試需要真實的 JDA 實例
    // 在實際環境中，需要使用 Test Discord 伺服器
    // 這裡僅作為模板，實際執行需要配置真實的 Discord Token
  }

  @AfterEach
  void tearDown() {
    ToolExecutionContext.clearContext();
    JDAProvider.clear();
  }

  @Test
  @Disabled("需要真實的 Discord 環境和配置")
  @DisplayName("整合測試範例（需要真實環境）")
  void integrationTestExample() {
    // 注意：此測試需要真實的 Discord 環境
    // 在 CI/CD 中應該跳過或使用 mock

    // Given
    // 設置真實的 guildId, channelId, userId
    // long guildId = ...;
    // long channelId = ...;
    // long userId = ...;

    // ToolExecutionContext.setContext(guildId, channelId, userId);

    // When
    // InvocationParameters params = createMockParameters(guildId, channelId, userId);
    // String result = tool.getChannelPermissions(String.valueOf(channelId), params);

    // Then
    // assertThat(result).contains("\"success\": true");
  }
}
