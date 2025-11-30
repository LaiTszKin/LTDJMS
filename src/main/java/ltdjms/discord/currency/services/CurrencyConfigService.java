package ltdjms.discord.currency.services;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing guild currency configuration.
 * Handles creating and updating currency name and icon per guild.
 */
public class CurrencyConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(CurrencyConfigService.class);

    private final GuildCurrencyConfigRepository configRepository;

    public CurrencyConfigService(GuildCurrencyConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * Gets the currency configuration for a guild.
     * Returns defaults if no custom configuration exists.
     *
     * @param guildId the Discord guild ID
     * @return the currency configuration
     */
    public GuildCurrencyConfig getConfig(long guildId) {
        return configRepository.findByGuildId(guildId)
                .orElse(GuildCurrencyConfig.createDefault(guildId));
    }

    /**
     * Updates the currency configuration for a guild.
     * Creates a new configuration if one doesn't exist.
     *
     * @param guildId the Discord guild ID
     * @param name    the new currency name (null to keep current)
     * @param icon    the new currency icon (null to keep current)
     * @return the updated configuration
     * @throws IllegalArgumentException if the name or icon is invalid
     */
    public GuildCurrencyConfig updateConfig(long guildId, String name, String icon) {
        LOG.debug("Updating currency config for guildId={}, name={}, icon={}", guildId, name, icon);

        // Validate inputs
        if (name != null) {
            validateName(name);
        }
        if (icon != null) {
            validateIcon(icon);
        }

        // Get existing config or create default
        GuildCurrencyConfig current = configRepository.findByGuildId(guildId)
                .orElse(GuildCurrencyConfig.createDefault(guildId));

        // Apply updates
        GuildCurrencyConfig updated = current.withUpdates(name, icon);

        // Save (upsert)
        GuildCurrencyConfig saved = configRepository.saveOrUpdate(updated);

        LOG.info("Updated currency config: guildId={}, name={}, icon={}",
                guildId, saved.currencyName(), saved.currencyIcon());

        return saved;
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Currency name cannot be blank");
        }
        if (name.length() > GuildCurrencyConfig.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Currency name cannot exceed " + GuildCurrencyConfig.MAX_NAME_LENGTH + " characters");
        }
    }

    private void validateIcon(String icon) {
        if (icon.isBlank()) {
            throw new IllegalArgumentException("Currency icon cannot be blank");
        }
        if (icon.length() > GuildCurrencyConfig.MAX_ICON_LENGTH) {
            throw new IllegalArgumentException(
                    "Currency icon cannot exceed " + GuildCurrencyConfig.MAX_ICON_LENGTH + " characters");
        }
    }
}
