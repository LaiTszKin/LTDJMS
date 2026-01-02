package ltdjms.discord.markdown.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MarkdownErrorFormatter - 錯誤報告格式化")
class MarkdownErrorFormatterTest {

  private final MarkdownErrorFormatter formatter = new MarkdownErrorFormatter();

  @Test
  @DisplayName("格式化的錯誤報告應包含所有必要資訊")
  void formattedErrorReport_shouldContainAllInformation() {
    String originalPrompt = "解釋 Java 中的 CompletableFuture";
    String fullResponse =
        """
        這是範例程式碼：

        ```java
        public class Test {
            public static void main(String[] args) {
                System.out.println("Hello");
            }
        """;

    java.util.List<MarkdownValidator.MarkdownError> errors =
        java.util.List.of(
            new MarkdownValidator.MarkdownError(
                MarkdownValidator.ErrorType.UNCLOSED_CODE_BLOCK,
                3,
                1,
                "```java",
                "在程式碼區塊結束處添加 ```"));

    String report = formatter.formatErrorReport(originalPrompt, errors, 1, fullResponse);

    // 驗證報告包含關鍵資訊
    assertTrue(report.contains("Markdown 格式驗證失敗"));
    assertTrue(report.contains("重試次數: 1"));
    assertTrue(report.contains("程式碼區塊未閉合"));
    assertTrue(report.contains("行 3"));
    assertTrue(report.contains("在程式碼區塊結束處添加 ```"));
  }

  @Test
  @DisplayName("多個錯誤應按類型分組顯示")
  void multipleErrors_shouldBeGroupedByType() {
    java.util.List<MarkdownValidator.MarkdownError> errors =
        java.util.List.of(
            new MarkdownValidator.MarkdownError(
                MarkdownValidator.ErrorType.UNCLOSED_CODE_BLOCK,
                3,
                1,
                "```java",
                "Add closing ```"),
            new MarkdownValidator.MarkdownError(
                MarkdownValidator.ErrorType.HEADING_LEVEL_EXCEEDED,
                10,
                1,
                "####### Too big",
                "Reduce to ######"));

    String report = formatter.formatErrorReport("test", errors, 1, "response");

    // 驗證兩個錯誤類型的標題都出現
    assertTrue(report.contains("程式碼區塊未閉合"));
    assertTrue(report.contains("標題層級超限"));
  }

  @Test
  @DisplayName("錯誤上下文截斷應正常工作")
  void longContext_shouldBeTruncated() {
    String longContext = "a".repeat(100);
    java.util.List<MarkdownValidator.MarkdownError> errors =
        java.util.List.of(
            new MarkdownValidator.MarkdownError(
                MarkdownValidator.ErrorType.MALFORMED_LIST, 1, 1, longContext, "fix it"));

    String report = formatter.formatErrorReport("test", errors, 1, "response");

    // 上下文不應過長
    assertFalse(report.contains(longContext));
  }
}
