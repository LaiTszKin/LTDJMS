package ltdjms.discord.currency.unit;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.services.CurrencyConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyConfigService.
 * Tests configuration retrieval, update, and validation logic.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyConfigServiceTest {

    private static final long TEST_GUILD_ID = 123456789012345678L;

    @Mock
    private GuildCurrencyConfigRepository configRepository;

    private CurrencyConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new CurrencyConfigService(configRepository);
    }

    @Test
    @DisplayName("should return default config when none exists")
    void shouldReturnDefaultConfigWhenNoneExists() {
        // Given
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());

        // When
        GuildCurrencyConfig config = configService.getConfig(TEST_GUILD_ID);

        // Then
        assertThat(config.guildId()).isEqualTo(TEST_GUILD_ID);
        assertThat(config.currencyName()).isEqualTo(GuildCurrencyConfig.DEFAULT_NAME);
        assertThat(config.currencyIcon()).isEqualTo(GuildCurrencyConfig.DEFAULT_ICON);
    }

    @Test
    @DisplayName("should return existing config when it exists")
    void shouldReturnExistingConfigWhenExists() {
        // Given
        Instant now = Instant.now();
        GuildCurrencyConfig existingConfig = new GuildCurrencyConfig(
                TEST_GUILD_ID, "Gold", "💰", now, now);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.of(existingConfig));

        // When
        GuildCurrencyConfig config = configService.getConfig(TEST_GUILD_ID);

        // Then
        assertThat(config.currencyName()).isEqualTo("Gold");
        assertThat(config.currencyIcon()).isEqualTo("💰");
    }

    @Test
    @DisplayName("should update config with new name")
    void shouldUpdateConfigWithNewName() {
        // Given
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());
        when(configRepository.saveOrUpdate(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        GuildCurrencyConfig updated = configService.updateConfig(TEST_GUILD_ID, "Gold", null);

        // Then
        assertThat(updated.currencyName()).isEqualTo("Gold");
        assertThat(updated.currencyIcon()).isEqualTo(GuildCurrencyConfig.DEFAULT_ICON);
        verify(configRepository).saveOrUpdate(any());
    }

    @Test
    @DisplayName("should update config with new icon")
    void shouldUpdateConfigWithNewIcon() {
        // Given
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());
        when(configRepository.saveOrUpdate(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        GuildCurrencyConfig updated = configService.updateConfig(TEST_GUILD_ID, null, "💰");

        // Then
        assertThat(updated.currencyName()).isEqualTo(GuildCurrencyConfig.DEFAULT_NAME);
        assertThat(updated.currencyIcon()).isEqualTo("💰");
    }

    @Test
    @DisplayName("should update both name and icon")
    void shouldUpdateBothNameAndIcon() {
        // Given
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());
        when(configRepository.saveOrUpdate(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        GuildCurrencyConfig updated = configService.updateConfig(TEST_GUILD_ID, "Diamonds", "💎");

        // Then
        assertThat(updated.currencyName()).isEqualTo("Diamonds");
        assertThat(updated.currencyIcon()).isEqualTo("💎");
    }

    @Test
    @DisplayName("should reject blank name")
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, "", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject name exceeding maximum length")
    void shouldRejectNameExceedingMaxLength() {
        String tooLongName = "A".repeat(51);

        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, tooLongName, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50");
    }

    @Test
    @DisplayName("should reject blank icon")
    void shouldRejectBlankIcon() {
        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, null, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject icon exceeding maximum length")
    void shouldRejectIconExceedingMaxLength() {
        String tooLongIcon = "A".repeat(33);

        assertThatThrownBy(() -> configService.updateConfig(TEST_GUILD_ID, null, tooLongIcon))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("should preserve existing values when updating only one field")
    void shouldPreserveExistingValuesWhenUpdatingOneField() {
        // Given - existing config with custom values
        Instant now = Instant.now();
        GuildCurrencyConfig existingConfig = new GuildCurrencyConfig(
                TEST_GUILD_ID, "Gold", "💰", now, now);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.of(existingConfig));
        when(configRepository.saveOrUpdate(any())).thenAnswer(inv -> inv.getArgument(0));

        // When - update only name
        GuildCurrencyConfig updated = configService.updateConfig(TEST_GUILD_ID, "Silver", null);

        // Then
        assertThat(updated.currencyName()).isEqualTo("Silver");
        assertThat(updated.currencyIcon()).isEqualTo("💰"); // Preserved
    }

    @Test
    @DisplayName("should isolate config between guilds")
    void shouldIsolateConfigBetweenGuilds() {
        // Given - two different guilds
        long guild1 = TEST_GUILD_ID;
        long guild2 = TEST_GUILD_ID + 1;

        when(configRepository.findByGuildId(guild1)).thenReturn(Optional.empty());
        when(configRepository.findByGuildId(guild2)).thenReturn(Optional.empty());

        // When
        GuildCurrencyConfig config1 = configService.getConfig(guild1);
        GuildCurrencyConfig config2 = configService.getConfig(guild2);

        // Then
        assertThat(config1.guildId()).isEqualTo(guild1);
        assertThat(config2.guildId()).isEqualTo(guild2);
        verify(configRepository).findByGuildId(guild1);
        verify(configRepository).findByGuildId(guild2);
    }
}
