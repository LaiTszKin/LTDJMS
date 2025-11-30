package ltdjms.discord.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Loads configuration from environment variables with fallback to .env file,
 * application.properties, and built-in defaults.
 *
 * <p>Priority order (highest to lowest):
 * <ol>
 *   <li>System environment variables</li>
 *   <li>.env file in project root</li>
 *   <li>application.properties</li>
 *   <li>Built-in defaults</li>
 * </ol>
 */
public final class EnvironmentConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);

    private static final String PROPERTIES_FILE = "application.properties";

    // Environment variable names
    private static final String ENV_DISCORD_BOT_TOKEN = "DISCORD_BOT_TOKEN";
    private static final String ENV_DB_URL = "DB_URL";
    private static final String ENV_DB_USERNAME = "DB_USERNAME";
    private static final String ENV_DB_PASSWORD = "DB_PASSWORD";
    private static final String ENV_DB_POOL_MAX_SIZE = "DB_POOL_MAX_SIZE";
    private static final String ENV_DB_POOL_MIN_IDLE = "DB_POOL_MIN_IDLE";
    private static final String ENV_DB_POOL_CONNECTION_TIMEOUT = "DB_POOL_CONNECTION_TIMEOUT";
    private static final String ENV_DB_POOL_IDLE_TIMEOUT = "DB_POOL_IDLE_TIMEOUT";
    private static final String ENV_DB_POOL_MAX_LIFETIME = "DB_POOL_MAX_LIFETIME";

    // Default values
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/currency_bot";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";
    private static final int DEFAULT_POOL_MAX_SIZE = 10;
    private static final int DEFAULT_POOL_MIN_IDLE = 2;
    private static final long DEFAULT_POOL_CONNECTION_TIMEOUT = 30000L;
    private static final long DEFAULT_POOL_IDLE_TIMEOUT = 600000L;
    private static final long DEFAULT_POOL_MAX_LIFETIME = 1800000L;

    private final Properties properties;
    private final Map<String, String> dotEnvValues;

    /**
     * Creates an EnvironmentConfig loading .env from the current working directory.
     */
    public EnvironmentConfig() {
        this(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Creates an EnvironmentConfig loading .env from the specified directory.
     *
     * @param dotEnvDirectory the directory containing the .env file
     */
    public EnvironmentConfig(Path dotEnvDirectory) {
        this.dotEnvValues = new DotEnvLoader(dotEnvDirectory).load();
        this.properties = loadProperties();
        if (!dotEnvValues.isEmpty()) {
            LOG.info("Loaded {} values from .env file", dotEnvValues.size());
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                props.load(input);
                LOG.debug("Loaded configuration from {}", PROPERTIES_FILE);
            } else {
                LOG.debug("No {} found, using environment variables and defaults", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            LOG.warn("Failed to load {}, using environment variables and defaults", PROPERTIES_FILE, e);
        }
        return props;
    }

    /**
     * Gets the Discord bot token. This value must be set via environment variable.
     *
     * @return the Discord bot token
     * @throws IllegalStateException if the token is not set
     */
    public String getDiscordBotToken() {
        String token = getEnvOrProperty(ENV_DISCORD_BOT_TOKEN, "discord.bot.token", null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Discord bot token not configured. Set " + ENV_DISCORD_BOT_TOKEN + " environment variable.");
        }
        return token;
    }

    /**
     * Gets the database JDBC URL.
     *
     * @return the database URL
     */
    public String getDatabaseUrl() {
        return getEnvOrProperty(ENV_DB_URL, "db.url", DEFAULT_DB_URL);
    }

    /**
     * Gets the database username.
     *
     * @return the database username
     */
    public String getDatabaseUsername() {
        return getEnvOrProperty(ENV_DB_USERNAME, "db.username", DEFAULT_DB_USERNAME);
    }

    /**
     * Gets the database password.
     *
     * @return the database password
     */
    public String getDatabasePassword() {
        return getEnvOrProperty(ENV_DB_PASSWORD, "db.password", DEFAULT_DB_PASSWORD);
    }

    /**
     * Gets the maximum connection pool size.
     *
     * @return the maximum pool size
     */
    public int getPoolMaxSize() {
        return getEnvOrPropertyAsInt(ENV_DB_POOL_MAX_SIZE, "db.pool.maximum-pool-size", DEFAULT_POOL_MAX_SIZE);
    }

    /**
     * Gets the minimum idle connections in the pool.
     *
     * @return the minimum idle connections
     */
    public int getPoolMinIdle() {
        return getEnvOrPropertyAsInt(ENV_DB_POOL_MIN_IDLE, "db.pool.minimum-idle", DEFAULT_POOL_MIN_IDLE);
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the connection timeout
     */
    public long getPoolConnectionTimeout() {
        return getEnvOrPropertyAsLong(ENV_DB_POOL_CONNECTION_TIMEOUT, "db.pool.connection-timeout", DEFAULT_POOL_CONNECTION_TIMEOUT);
    }

    /**
     * Gets the idle connection timeout in milliseconds.
     *
     * @return the idle timeout
     */
    public long getPoolIdleTimeout() {
        return getEnvOrPropertyAsLong(ENV_DB_POOL_IDLE_TIMEOUT, "db.pool.idle-timeout", DEFAULT_POOL_IDLE_TIMEOUT);
    }

    /**
     * Gets the maximum connection lifetime in milliseconds.
     *
     * @return the max lifetime
     */
    public long getPoolMaxLifetime() {
        return getEnvOrPropertyAsLong(ENV_DB_POOL_MAX_LIFETIME, "db.pool.max-lifetime", DEFAULT_POOL_MAX_LIFETIME);
    }

    private String getEnvOrProperty(String envName, String propertyName, String defaultValue) {
        // Priority 1: System environment variable
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        // Priority 2: .env file
        String dotEnvValue = dotEnvValues.get(envName);
        if (dotEnvValue != null && !dotEnvValue.isBlank()) {
            return dotEnvValue;
        }

        // Priority 3: application.properties, then default
        return properties.getProperty(propertyName, defaultValue);
    }

    private int getEnvOrPropertyAsInt(String envName, String propertyName, int defaultValue) {
        String value = getEnvOrProperty(envName, propertyName, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer value for {}: {}, using default: {}", envName, value, defaultValue);
            return defaultValue;
        }
    }

    private long getEnvOrPropertyAsLong(String envName, String propertyName, long defaultValue) {
        String value = getEnvOrProperty(envName, propertyName, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid long value for {}: {}, using default: {}", envName, value, defaultValue);
            return defaultValue;
        }
    }
}
