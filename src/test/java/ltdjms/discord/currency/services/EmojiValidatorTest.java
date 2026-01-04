package ltdjms.discord.currency.services;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmojiValidator")
class EmojiValidatorTest {

  @Nested
  @DisplayName("NoOpEmojiValidator")
  class NoOpEmojiValidatorTests {

    private final EmojiValidator validator = new NoOpEmojiValidator();

    @Test
    @DisplayName("should return false for null input")
    void shouldReturnFalseForNull() {
      assertThat(validator.isValidCustomEmoji(null)).isFalse();
    }

    @Test
    @DisplayName("should return false for empty string")
    void shouldReturnFalseForEmpty() {
      assertThat(validator.isValidCustomEmoji("")).isFalse();
    }

    @Test
    @DisplayName("should return false for blank string")
    void shouldReturnFalseForBlank() {
      assertThat(validator.isValidCustomEmoji("   ")).isFalse();
    }

    @Test
    @DisplayName("should return true for valid static custom emoji")
    void shouldReturnTrueForValidStaticEmoji() {
      assertThat(validator.isValidCustomEmoji("<:testemoji:123456789012345678>")).isTrue();
    }

    @Test
    @DisplayName("should return true for valid animated custom emoji")
    void shouldReturnTrueForValidAnimatedEmoji() {
      assertThat(validator.isValidCustomEmoji("<a:testemoji:123456789012345678>")).isTrue();
    }

    @Test
    @DisplayName("should return false for emoji without opening bracket")
    void shouldReturnFalseForMissingOpeningBracket() {
      assertThat(validator.isValidCustomEmoji(":testemoji:123456789012345678>")).isFalse();
    }

    @Test
    @DisplayName("should return false for emoji without closing bracket")
    void shouldReturnFalseForMissingClosingBracket() {
      assertThat(validator.isValidCustomEmoji("<:testemoji:123456789012345678")).isFalse();
    }

    @Test
    @DisplayName("should return false for emoji with name shorter than 2 characters")
    void shouldReturnFalseForShortName() {
      assertThat(validator.isValidCustomEmoji("<:a:123456789012345678>")).isFalse();
    }

    @Test
    @DisplayName("should return false for emoji with name longer than 32 characters")
    void shouldReturnFalseForLongName() {
      String longName = "a".repeat(33);
      assertThat(validator.isValidCustomEmoji("<:" + longName + ":123456789012345678>")).isFalse();
    }

    @Test
    @DisplayName("should return false for emoji with ID shorter than 17 digits")
    void shouldReturnFalseForShortId() {
      // 16 digits is too short (minimum is 17 for Discord snowflake)
      assertThat(validator.isValidCustomEmoji("<:testemoji:1234567890123456>")).isFalse();
    }

    @Test
    @DisplayName("should return true for emoji with exactly 17 digit ID")
    void shouldReturnTrueForExactly17Digits() {
      // 17 digits is the minimum valid length
      assertThat(validator.isValidCustomEmoji("<:testemoji:12345678901234567>")).isTrue();
    }

    @Test
    @DisplayName("should return false for emoji with ID longer than 20 digits")
    void shouldReturnFalseForLongId() {
      assertThat(validator.isValidCustomEmoji("<:testemoji:123456789012345678901>")).isFalse();
    }

    @Test
    @DisplayName("should return false for emoji with non-word characters in name")
    void shouldReturnFalseForNonWordCharacters() {
      assertThat(validator.isValidCustomEmoji("<:test-emoji:123456789012345678>")).isFalse();
    }

    @Test
    @DisplayName("should return true for emoji with underscores in name")
    void shouldReturnTrueForUnderscores() {
      assertThat(validator.isValidCustomEmoji("<:test_emoji:123456789012345678>")).isTrue();
    }

    @Test
    @DisplayName("should return true for emoji with numbers in name")
    void shouldReturnTrueForNumbersInName() {
      assertThat(validator.isValidCustomEmoji("<:test123:123456789012345678>")).isTrue();
    }

    @Test
    @DisplayName("should return false for plain text emoji")
    void shouldReturnFalseForPlainTextEmoji() {
      assertThat(validator.isValidCustomEmoji("😀")).isFalse();
    }

    @Test
    @DisplayName("should return false for unicode emoji")
    void shouldReturnFalseForUnicodeEmoji() {
      assertThat(validator.isValidCustomEmoji("🎉")).isFalse();
    }
  }
}
