package ltdjms.discord.aichat.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ltdjms.discord.aichat.services.MessageSplitter;

/** 測試 {@link MessageSplitter} 的訊息分割功能。 */
class MessageSplitterTest {

  @Test
  void testShortMessage_shouldNotSplit() {
    // Given
    String shortMessage = "這是一則短訊息";

    // When
    List<String> result = MessageSplitter.split(shortMessage);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(shortMessage);
  }

  @Test
  void testParagraphSplit_shouldSplitAtNewlines() {
    // Given
    String message = "第一段文字\n\n第二段文字\n\n第三段文字";

    // When
    List<String> result = MessageSplitter.split(message);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0)).isEqualTo("第一段文字");
    assertThat(result.get(1)).isEqualTo("第二段文字");
    assertThat(result.get(2)).isEqualTo("第三段文字");
  }

  @Test
  void testSentenceSplit_shouldSplitAtSentenceBoundaries() {
    // Given
    String message = "這是第一句。這是第二句！這是第三句？";

    // When
    List<String> result = MessageSplitter.split(message);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0)).isEqualTo("這是第一句。");
    assertThat(result.get(1)).isEqualTo("這是第二句！");
    assertThat(result.get(2)).isEqualTo("這是第三句？");
  }

  @Test
  void testForcedSplit_shouldSplitWhenNoBoundaryFound() {
    // Given
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2000; i++) {
      sb.append("a");
    }
    String longMessage = sb.toString();

    // When
    List<String> result = MessageSplitter.split(longMessage);

    // Then
    assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    // Verify each part is within the limit
    for (String part : result) {
      assertThat(part.length()).isLessThanOrEqualTo(1980);
    }
  }

  @Test
  void testEmptyMessage_shouldReturnSingleEmptyString() {
    // Given
    String emptyMessage = "";

    // When
    List<String> result = MessageSplitter.split(emptyMessage);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEmpty();
  }

  @Test
  void testNullMessage_shouldReturnSingleEmptyString() {
    // Given
    String nullMessage = null;

    // When
    List<String> result = MessageSplitter.split(nullMessage);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEmpty();
  }
}
