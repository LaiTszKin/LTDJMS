package ltdjms.discord.currency.unit;

import ltdjms.discord.currency.domain.GuildCurrencyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for currency icon length relaxation as specified in CURRENCY-CONFIG-ICON requirements.
 * The maximum icon length is increased from 10 to 32 characters.
 */
class CurrencyIconLengthTest {

    private static final long TEST_GUILD_ID = 123456789012345678L;

    @Nested
    @DisplayName("Currency Icon Length Validation")
    class IconLengthValidation {

        @Test
        @DisplayName("should accept icon with exactly 1 character")
        void shouldAcceptIconWithOneCharacter() {
            Instant now = Instant.now();
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Gold", "💰", now, now);

            assertThat(config.currencyIcon()).isEqualTo("💰");
        }

        @Test
        @DisplayName("should accept icon with multi-codepoint emoji")
        void shouldAcceptIconWithMultiCodepointEmoji() {
            Instant now = Instant.now();
            // 👨‍👩‍👧‍👦 is a family emoji with multiple code points (11 Java chars)
            String familyEmoji = "👨‍👩‍👧‍👦";
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Family Coins", familyEmoji, now, now);

            assertThat(config.currencyIcon()).isEqualTo(familyEmoji);
        }

        @Test
        @DisplayName("should accept icon with text and emoji combination")
        void shouldAcceptIconWithTextAndEmojiCombination() {
            Instant now = Instant.now();
            // "💎 Points" is a common use case (9 characters)
            String iconWithText = "💎 Points";
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Gems", iconWithText, now, now);

            assertThat(config.currencyIcon()).isEqualTo(iconWithText);
        }

        @Test
        @DisplayName("should accept icon with exactly 32 characters")
        void shouldAcceptIconWithExactly32Characters() {
            Instant now = Instant.now();
            String icon32Chars = "a".repeat(32);
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Currency", icon32Chars, now, now);

            assertThat(config.currencyIcon()).hasSize(32);
        }

        @Test
        @DisplayName("should reject icon exceeding 32 characters")
        void shouldRejectIconExceeding32Characters() {
            Instant now = Instant.now();
            String tooLongIcon = "a".repeat(33);

            assertThatThrownBy(() -> new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Currency", tooLongIcon, now, now))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceed")
                    .hasMessageContaining("32");
        }

        @Test
        @DisplayName("should reject blank icon")
        void shouldRejectBlankIcon() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Currency", "", now, now))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should reject whitespace-only icon")
        void shouldRejectWhitespaceOnlyIcon() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Currency", "   ", now, now))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("should accept Chinese text as icon")
        void shouldAcceptChineseTextAsIcon() {
            Instant now = Instant.now();
            String chineseIcon = "金幣 💰";
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Gold", chineseIcon, now, now);

            assertThat(config.currencyIcon()).isEqualTo(chineseIcon);
        }

        @Test
        @DisplayName("should accept flag emoji")
        void shouldAcceptFlagEmoji() {
            Instant now = Instant.now();
            // Flag emojis are typically 2 regional indicator characters (4 Java chars)
            String flagEmoji = "🇺🇸";
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "USD", flagEmoji, now, now);

            assertThat(config.currencyIcon()).isEqualTo(flagEmoji);
        }

        @Test
        @DisplayName("should accept skin tone modified emoji")
        void shouldAcceptSkinToneModifiedEmoji() {
            Instant now = Instant.now();
            // Skin tone modified emoji (multiple code points)
            String skinToneEmoji = "👍🏽";
            GuildCurrencyConfig config = new GuildCurrencyConfig(
                    TEST_GUILD_ID, "Likes", skinToneEmoji, now, now);

            assertThat(config.currencyIcon()).isEqualTo(skinToneEmoji);
        }
    }

    @Nested
    @DisplayName("MAX_ICON_LENGTH Constant")
    class MaxIconLengthConstant {

        @Test
        @DisplayName("MAX_ICON_LENGTH should be 32")
        void maxIconLengthShouldBe32() {
            assertThat(GuildCurrencyConfig.MAX_ICON_LENGTH).isEqualTo(32);
        }
    }
}
