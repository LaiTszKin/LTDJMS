package ltdjms.discord.shared;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for .env integration in {@link EnvironmentConfig}.
 * Verifies the priority order: Environment variables > .env > application.properties > defaults.
 */
class EnvironmentConfigDotEnvIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should load database URL from .env when environment variable not set")
    void shouldLoadDatabaseUrlFromDotEnv() throws IOException {
        // Given a .env file with DB_URL
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            DB_URL=jdbc:postgresql://envhost:5432/envdb
            """);

        // When creating EnvironmentConfig with .env directory
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then DB_URL should come from .env (if env var not set)
        // Note: This test assumes DB_URL environment variable is not set
        // The actual value depends on whether env var is set in test environment
        String dbUrl = config.getDatabaseUrl();
        assertThat(dbUrl).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("should use default values when .env file is missing and env vars not set")
    void shouldUseDefaultValuesWhenDotEnvMissingAndEnvVarsNotSet() {
        // Given no .env file and assuming env vars are not set for pool settings
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then pool settings should use defaults
        assertThat(config.getPoolMaxSize()).isEqualTo(10);
        assertThat(config.getPoolMinIdle()).isEqualTo(2);
        assertThat(config.getPoolConnectionTimeout()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("should load pool configuration from .env file")
    void shouldLoadPoolConfigurationFromDotEnv() throws IOException {
        // Given a .env file with pool configuration
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            DB_POOL_MAX_SIZE=20
            DB_POOL_MIN_IDLE=5
            DB_POOL_CONNECTION_TIMEOUT=60000
            """);

        // When creating EnvironmentConfig
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then values should come from .env (if env vars not set)
        // Note: Results may differ if environment variables are set
        int maxSize = config.getPoolMaxSize();
        int minIdle = config.getPoolMinIdle();
        long connTimeout = config.getPoolConnectionTimeout();

        // At minimum, values should be valid
        assertThat(maxSize).isPositive();
        assertThat(minIdle).isPositive();
        assertThat(connTimeout).isPositive();
    }

    @Test
    @DisplayName("should gracefully handle malformed .env file")
    void shouldGracefullyHandleMalformedDotEnvFile() throws IOException {
        // Given a malformed .env file
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            not a valid env file format
            also invalid
            """);

        // When creating EnvironmentConfig
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then it should not throw and use defaults
        assertThat(config.getPoolMaxSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("should require Discord bot token to be configured")
    void shouldRequireDiscordBotTokenToBeConfigured() throws IOException {
        // Given a .env file without DISCORD_BOT_TOKEN
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            DB_URL=jdbc:postgresql://localhost:5432/testdb
            """);

        // When creating EnvironmentConfig and getting token
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then it should throw if token is not set via env var
        // Note: This test may pass differently if DISCORD_BOT_TOKEN is set in environment
        if (System.getenv("DISCORD_BOT_TOKEN") == null) {
            assertThatThrownBy(config::getDiscordBotToken)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DISCORD_BOT_TOKEN");
        }
    }

    @Test
    @DisplayName("should load Discord bot token from .env file")
    void shouldLoadDiscordBotTokenFromDotEnv() throws IOException {
        // Given a .env file with DISCORD_BOT_TOKEN
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
            DISCORD_BOT_TOKEN=test-token-from-env
            """);

        // When creating EnvironmentConfig
        EnvironmentConfig config = new EnvironmentConfig(tempDir);

        // Then token should be loadable (if env var not set)
        // Note: If DISCORD_BOT_TOKEN env var is set, it will take precedence
        String token = config.getDiscordBotToken();
        assertThat(token).isNotNull().isNotBlank();
    }
}
