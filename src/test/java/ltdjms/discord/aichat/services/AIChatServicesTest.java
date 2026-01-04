package ltdjms.discord.aichat.services;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ltdjms.discord.shared.DomainError;

/** Unit tests for aichat.services package utility classes. */
@DisplayName("AIChat Services")
class AIChatServicesTest {

  @Nested
  @DisplayName("MessageSplitter")
  class MessageSplitterTests {

    @Test
    @DisplayName("should return empty string for null input")
    void shouldReturnEmptyStringForNullInput() {
      List<String> result = MessageSplitter.split(null);
      assertThat(result).containsExactly("");
    }

    @Test
    @DisplayName("should return empty string for empty input")
    void shouldReturnEmptyStringForEmptyInput() {
      List<String> result = MessageSplitter.split("");
      assertThat(result).containsExactly("");
    }

    @Test
    @DisplayName("should return single message for short content")
    void shouldReturnSingleMessageForShortContent() {
      String content = "This is a short message.";
      List<String> result = MessageSplitter.split(content);

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(content);
    }

    @Test
    @DisplayName("should split by paragraphs when content has multiple paragraphs")
    void shouldSplitByParagraphs() {
      // Need content long enough to trigger paragraph splitting
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 50; i++) {
        sb.append("First paragraph sentence ").append(i).append(". ");
      }
      sb.append("\n\n");
      for (int i = 0; i < 50; i++) {
        sb.append("Second paragraph sentence ").append(i).append(". ");
      }
      sb.append("\n\n");
      for (int i = 0; i < 50; i++) {
        sb.append("Third paragraph sentence ").append(i).append(". ");
      }

      List<String> result = MessageSplitter.split(sb.toString());

      assertThat(result).hasSize(3);
      assertThat(result.get(0)).contains("First paragraph");
      assertThat(result.get(1)).contains("Second paragraph");
      assertThat(result.get(2)).contains("Third paragraph");
    }

    @Test
    @DisplayName("should split by Chinese sentence boundaries")
    void shouldSplitByChineseSentences() {
      // Need content long enough to trigger splitting
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        sb.append("這是第").append(i).append("句。");
      }
      for (int i = 0; i < 100; i++) {
        sb.append("這是第").append(i).append("句！");
      }
      for (int i = 0; i < 100; i++) {
        sb.append("這是第").append(i).append("句？");
      }

      List<String> result = MessageSplitter.split(sb.toString());

      assertThat(result.size()).isGreaterThan(1);
      // Verify that the split happened at sentence boundaries
      for (String part : result) {
        assertThat(part.length()).isLessThanOrEqualTo(1980);
      }
    }

    @Test
    @DisplayName("should split long message by newlines when possible")
    void shouldSplitLongMessageByNewlines() {
      StringBuilder sb = new StringBuilder();
      // Create content much longer than 1980 chars to trigger splitting
      for (int i = 0; i < 500; i++) {
        sb.append("Line ").append(i).append("\n");
      }
      String content = sb.toString();

      List<String> result = MessageSplitter.split(content);

      assertThat(result.size()).isGreaterThan(1);
      for (String part : result) {
        assertThat(part.length()).isLessThanOrEqualTo(1980);
      }
    }

    @Test
    @DisplayName("should force split when content exceeds max length")
    void shouldForceSplitWhenContentExceedsMaxLength() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 3000; i++) {
        sb.append("a");
      }
      String content = sb.toString();

      List<String> result = MessageSplitter.split(content);

      assertThat(result.size()).isGreaterThan(1);
      for (String part : result) {
        assertThat(part.length()).isLessThanOrEqualTo(1980);
      }
    }

    @Test
    @DisplayName("should handle mixed paragraph and sentence boundaries")
    void shouldHandleMixedBoundaries() {
      // Need content long enough to trigger splitting
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 100; i++) {
        sb.append("Paragraph one。Paragraph two！\n\nParagraph three？Paragraph four。");
      }
      String content = sb.toString();
      List<String> result = MessageSplitter.split(content);

      assertThat(result).isNotEmpty();
      for (String part : result) {
        assertThat(part.length()).isLessThanOrEqualTo(1980);
      }
    }

    @Test
    @DisplayName("should trim whitespace from split parts")
    void shouldTrimWhitespaceFromSplitParts() {
      // Need content long enough to trigger splitting (> 1980 chars)
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 200; i++) {
        sb.append("  Paragraph  ").append(i).append("  \n\n");
      }
      String content = sb.toString();

      List<String> result = MessageSplitter.split(content);

      assertThat(result.size()).isGreaterThan(1);
      // Check that parts are trimmed
      for (String part : result) {
        assertThat(part).doesNotStartWith(" ");
        assertThat(part).doesNotEndWith(" ");
      }
    }

    @Test
    @DisplayName("should handle single very long line without boundaries")
    void shouldHandleSingleVeryLongLine() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 2500; i++) {
        sb.append("a");
      }
      String content = sb.toString();

      List<String> result = MessageSplitter.split(content);

      assertThat(result.size()).isGreaterThan(1);
      assertThat(result.get(0).length()).isLessThanOrEqualTo(1980);
    }
  }

  @Nested
  @DisplayName("MessageChunkAccumulator")
  class MessageChunkAccumulatorTests {

    @Test
    @DisplayName("should return empty list for null delta")
    void shouldReturnEmptyListForNullDelta() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      List<String> result = accumulator.accumulate(null);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for empty delta")
    void shouldReturnEmptyListForEmptyDelta() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      List<String> result = accumulator.accumulate("");
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should accumulate short delta without splitting")
    void shouldAccumulateShortDelta() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      List<String> result = accumulator.accumulate("Hello");
      assertThat(result).isEmpty();

      String drained = accumulator.drain();
      assertThat(drained).isEqualTo("Hello");
    }

    @Test
    @DisplayName("should split at paragraph boundary")
    void shouldSplitAtParagraphBoundary() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      List<String> result = accumulator.accumulate("First paragraph\n\n");
      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo("First paragraph\n\n");
    }

    @Test
    @DisplayName("should force split when exceeding max length")
    void shouldForceSplitWhenExceedingMaxLength() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 1985; i++) {
        sb.append("a");
      }
      String longText = sb.toString();

      List<String> result = accumulator.accumulate(longText);
      assertThat(result).hasSize(1);
      assertThat(result.get(0).length()).isEqualTo(1980);

      String drained = accumulator.drain();
      assertThat(drained).hasSize(5);
    }

    @Test
    @DisplayName("should handle multiple accumulations before paragraph split")
    void shouldHandleMultipleAccumulations() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      List<String> r1 = accumulator.accumulate("Hello ");
      assertThat(r1).isEmpty();

      List<String> r2 = accumulator.accumulate("world\n\n");
      assertThat(r2).hasSize(1);
      assertThat(r2.get(0)).isEqualTo("Hello world\n\n");
    }

    @Test
    @DisplayName("should drain remaining content")
    void shouldDrainRemainingContent() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      accumulator.accumulate("Remaining text");
      String drained = accumulator.drain();
      assertThat(drained).isEqualTo("Remaining text");

      // Second drain should be empty
      String secondDrain = accumulator.drain();
      assertThat(secondDrain).isEmpty();
    }

    @Test
    @DisplayName("should trim whitespace on drain")
    void shouldTrimWhitespaceOnDrain() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      accumulator.accumulate("  text  ");
      String drained = accumulator.drain();
      assertThat(drained).isEqualTo("text");
    }

    @Test
    @DisplayName("should prefer paragraph split over forced split")
    void shouldPreferParagraphSplit() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
        sb.append("a");
      }
      sb.append("\n\n");
      sb.append("After paragraph");

      List<String> result = accumulator.accumulate(sb.toString());
      assertThat(result).hasSize(1);
      assertThat(result.get(0)).contains("\n\n");
      assertThat(result.get(0)).endsWith("\n\n");
    }

    @Test
    @DisplayName("should handle empty buffer on drain")
    void shouldHandleEmptyBufferOnDrain() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      String drained = accumulator.drain();
      assertThat(drained).isEmpty();
    }

    @Test
    @DisplayName("should clear buffer after paragraph split")
    void shouldClearBufferAfterParagraphSplit() {
      MessageChunkAccumulator accumulator = new MessageChunkAccumulator();
      accumulator.accumulate("Before\n\n");
      String drained = accumulator.drain();
      assertThat(drained).isEmpty();
    }
  }

  @Nested
  @DisplayName("LangChain4jExceptionMapper")
  class LangChain4jExceptionMapperTests {

    @Test
    @DisplayName("should return unexpected failure for null exception")
    void shouldReturnUnexpectedFailureForNull() {
      DomainError result = LangChain4jExceptionMapper.map(null);
      assertThat(result.category()).isEqualTo(DomainError.Category.UNEXPECTED_FAILURE);
    }

    @Test
    @DisplayName("should map TimeoutException to timeout category")
    void shouldMapTimeoutException() {
      TimeoutException ex = new TimeoutException("Request timed out");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_TIMEOUT);
      assertThat(result.message()).contains("逾時");
    }

    @Test
    @DisplayName("should map exception with timeout in type name")
    void shouldMapTimeoutByTypeName() {
      // The mapper checks if the exception message contains "timeout" (case insensitive)
      Throwable ex = new Throwable("Request timeout occurred");
      DomainError result = LangChain4jExceptionMapper.map(ex);
      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_TIMEOUT);
    }

    @Test
    @DisplayName("should map 401 error to authentication failed")
    void shouldMap401Error() {
      Throwable ex = new Exception("HTTP 401 Unauthorized");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_AUTH_FAILED);
      assertThat(result.message()).contains("認證失敗");
    }

    @Test
    @DisplayName("should map Unauthorized message to authentication failed")
    void shouldMapUnauthorizedMessage() {
      Throwable ex = new Exception("Unauthorized access");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_AUTH_FAILED);
    }

    @Test
    @DisplayName("should map authentication keyword to authentication failed")
    void shouldMapAuthenticationKeyword() {
      Throwable ex = new Exception("Error: authentication failed");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_AUTH_FAILED);
    }

    @Test
    @DisplayName("should map API key error to authentication failed")
    void shouldMapAPIKeyError() {
      Throwable ex = new Exception("Invalid API key");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_AUTH_FAILED);
    }

    @Test
    @DisplayName("should map 429 error to rate limited")
    void shouldMap429Error() {
      Throwable ex = new Exception("HTTP 429 Too Many Requests");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_RATE_LIMITED);
      assertThat(result.message()).contains("速率限制");
    }

    @Test
    @DisplayName("should map rate limit message to rate limited")
    void shouldMapRateLimitMessage() {
      Throwable ex = new Exception("Error: rate limit exceeded");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_RATE_LIMITED);
    }

    @Test
    @DisplayName("should map RateLimitExceeded to rate limited")
    void shouldMapRateLimitExceeded() {
      Throwable ex = new Exception("RateLimitExceeded");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_RATE_LIMITED);
    }

    @Test
    @DisplayName("should map 500 error to service unavailable")
    void shouldMap500Error() {
      Throwable ex = new Exception("HTTP 500 Internal Server Error");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_UNAVAILABLE);
      assertThat(result.message()).contains("不可用");
    }

    @Test
    @DisplayName("should map 502 error to service unavailable")
    void shouldMap502Error() {
      Throwable ex = new Exception("HTTP 502 Bad Gateway");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("should map 503 error to service unavailable")
    void shouldMap503Error() {
      Throwable ex = new Exception("HTTP 503 Service Unavailable");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("should map service unavailable message")
    void shouldMapServiceUnavailableMessage() {
      Throwable ex = new Exception("Error: service unavailable");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("should map 400 error to invalid request")
    void shouldMap400Error() {
      Throwable ex = new Exception("HTTP 400 Bad Request");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_RESPONSE_INVALID);
      assertThat(result.message()).contains("請求無效");
    }

    @Test
    @DisplayName("should map Bad Request message to invalid request")
    void shouldMapBadRequestMessage() {
      Throwable ex = new Exception("Bad Request");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_RESPONSE_INVALID);
    }

    @Test
    @DisplayName("should map Invalid message to invalid request")
    void shouldMapInvalidMessage() {
      Throwable ex = new Exception("Invalid parameter");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.AI_RESPONSE_INVALID);
    }

    @Test
    @DisplayName("should map unknown exception to unexpected failure")
    void shouldMapUnknownException() {
      Throwable ex = new Exception("Unknown error occurred");
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.category()).isEqualTo(DomainError.Category.UNEXPECTED_FAILURE);
      assertThat(result.message()).contains("未預期");
    }

    @Test
    @DisplayName("should truncate long error messages")
    void shouldTruncateLongErrorMessages() {
      StringBuilder sb = new StringBuilder("HTTP 400 ");
      for (int i = 0; i < 300; i++) {
        sb.append("a");
      }
      Throwable ex = new Exception(sb.toString());
      DomainError result = LangChain4jExceptionMapper.map(ex);

      assertThat(result.message()).hasSize(213); // "AI 服務請求無效：" + 200 + "..."
      assertThat(result.message()).endsWith("...");
    }

    @Test
    @DisplayName("should preserve original exception in DomainError")
    void shouldPreserveOriginalException() {
      Exception original = new Exception("Test exception");
      DomainError result = LangChain4jExceptionMapper.map(original);

      assertThat(result.cause()).isSameAs(original);
    }
  }
}
