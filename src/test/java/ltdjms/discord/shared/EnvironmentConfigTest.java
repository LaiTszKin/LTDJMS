package ltdjms.discord.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentConfig}.
 *
 * These tests focus on default values when no environment overrides are present.
 */
class EnvironmentConfigTest {

    @Test
    void defaultDatabaseUrlUsesLocalCurrencyBotDatabase() {
        EnvironmentConfig config = new EnvironmentConfig();

        // When DB_URL is not set in the environment, we should fall back to the
        // hardcoded default JDBC URL instead of a placeholder literal.
        assertThat(config.getDatabaseUrl())
                .isEqualTo("jdbc:postgresql://localhost:5432/currency_bot");
    }
}

