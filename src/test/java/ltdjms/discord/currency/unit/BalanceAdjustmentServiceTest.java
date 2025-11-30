package ltdjms.discord.currency.unit;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import ltdjms.discord.currency.domain.MemberCurrencyAccount;
import ltdjms.discord.currency.persistence.GuildCurrencyConfigRepository;
import ltdjms.discord.currency.persistence.MemberCurrencyAccountRepository;
import ltdjms.discord.currency.persistence.NegativeBalanceException;
import ltdjms.discord.currency.services.BalanceAdjustmentService;
import ltdjms.discord.currency.services.BalanceAdjustmentService.BalanceAdjustmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BalanceAdjustmentService.
 * Tests credit, debit, and error handling including non-negative balance enforcement.
 */
@ExtendWith(MockitoExtension.class)
class BalanceAdjustmentServiceTest {

    private static final long TEST_GUILD_ID = 123456789012345678L;
    private static final long TEST_USER_ID = 987654321098765432L;

    @Mock
    private MemberCurrencyAccountRepository accountRepository;

    @Mock
    private GuildCurrencyConfigRepository configRepository;

    private BalanceAdjustmentService adjustmentService;

    @BeforeEach
    void setUp() {
        adjustmentService = new BalanceAdjustmentService(accountRepository, configRepository);
    }

    @Test
    @DisplayName("should credit balance successfully")
    void shouldCreditBalanceSuccessfully() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount initial = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 100L, now, now);
        MemberCurrencyAccount adjusted = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 150L, now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(initial);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 50L)).thenReturn(adjusted);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());

        // When
        BalanceAdjustmentResult result = adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 50L);

        // Then
        assertThat(result.previousBalance()).isEqualTo(100L);
        assertThat(result.newBalance()).isEqualTo(150L);
        assertThat(result.adjustment()).isEqualTo(50L);
    }

    @Test
    @DisplayName("should debit balance within limits")
    void shouldDebitBalanceWithinLimits() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount initial = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 100L, now, now);
        MemberCurrencyAccount adjusted = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 50L, now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(initial);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, -50L)).thenReturn(adjusted);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());

        // When
        BalanceAdjustmentResult result = adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, -50L);

        // Then
        assertThat(result.previousBalance()).isEqualTo(100L);
        assertThat(result.newBalance()).isEqualTo(50L);
        assertThat(result.adjustment()).isEqualTo(-50L);
    }

    @Test
    @DisplayName("should reject negative balance")
    void shouldRejectNegativeBalance() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount initial = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 50L, now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(initial);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, -100L))
                .thenThrow(new NegativeBalanceException("Insufficient balance"));

        // When/Then
        assertThatThrownBy(() -> adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, -100L))
                .isInstanceOf(NegativeBalanceException.class);
    }

    @Test
    @DisplayName("should reject amount exceeding maximum")
    void shouldRejectAmountExceedingMaximum() {
        long tooLargeAmount = MemberCurrencyAccount.MAX_ADJUSTMENT_AMOUNT + 1;

        assertThatThrownBy(() -> adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, tooLargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("should reject negative amount exceeding maximum")
    void shouldRejectNegativeAmountExceedingMaximum() {
        long tooLargeNegativeAmount = -(MemberCurrencyAccount.MAX_ADJUSTMENT_AMOUNT + 1);

        assertThatThrownBy(() -> adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, tooLargeNegativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("should include currency info in result")
    void shouldIncludeCurrencyInfoInResult() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount initial = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 0L, now, now);
        MemberCurrencyAccount adjusted = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 100L, now, now);
        GuildCurrencyConfig config = new GuildCurrencyConfig(TEST_GUILD_ID, "Gold", "💰", now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(initial);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L)).thenReturn(adjusted);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.of(config));

        // When
        BalanceAdjustmentResult result = adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L);

        // Then
        assertThat(result.currencyName()).isEqualTo("Gold");
        assertThat(result.currencyIcon()).isEqualTo("💰");
    }

    @Test
    @DisplayName("should use default currency when no config exists")
    void shouldUseDefaultCurrencyWhenNoConfigExists() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount initial = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 0L, now, now);
        MemberCurrencyAccount adjusted = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 100L, now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(initial);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L)).thenReturn(adjusted);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());

        // When
        BalanceAdjustmentResult result = adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L);

        // Then
        assertThat(result.currencyName()).isEqualTo(GuildCurrencyConfig.DEFAULT_NAME);
        assertThat(result.currencyIcon()).isEqualTo(GuildCurrencyConfig.DEFAULT_ICON);
    }

    @Test
    @DisplayName("should create account if not exists before adjustment")
    void shouldCreateAccountIfNotExistsBeforeAdjustment() {
        // Given
        Instant now = Instant.now();
        MemberCurrencyAccount newAccount = MemberCurrencyAccount.createNew(TEST_GUILD_ID, TEST_USER_ID);
        MemberCurrencyAccount adjusted = new MemberCurrencyAccount(TEST_GUILD_ID, TEST_USER_ID, 100L, now, now);

        when(accountRepository.findOrCreate(TEST_GUILD_ID, TEST_USER_ID)).thenReturn(newAccount);
        when(accountRepository.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L)).thenReturn(adjusted);
        when(configRepository.findByGuildId(TEST_GUILD_ID)).thenReturn(Optional.empty());

        // When
        BalanceAdjustmentResult result = adjustmentService.adjustBalance(TEST_GUILD_ID, TEST_USER_ID, 100L);

        // Then
        verify(accountRepository).findOrCreate(TEST_GUILD_ID, TEST_USER_ID);
        assertThat(result.previousBalance()).isEqualTo(0L);
        assertThat(result.newBalance()).isEqualTo(100L);
    }
}
