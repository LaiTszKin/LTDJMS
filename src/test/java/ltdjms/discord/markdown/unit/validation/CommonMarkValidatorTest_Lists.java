package ltdjms.discord.markdown.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommonMarkValidator - 列表格式驗證")
class CommonMarkValidatorTest_Lists {

  private final MarkdownValidator validator = new CommonMarkValidator();

  @Test
  @DisplayName("正確的無序列表應通過驗證")
  void validUnorderedList_shouldPass() {
    String markdown =
        """
        事項清單：

        - 項目一
        - 項目二
        - 項目三
        """;

    MarkdownValidator.ValidationResult result = validator.validate(markdown);

    assertInstanceOf(MarkdownValidator.ValidationResult.Valid.class, result);
  }

  @Test
  @DisplayName("正確的有序列表應通過驗證")
  void validOrderedList_shouldPass() {
    String markdown =
        """
        步驟：

        1. 第一步
        2. 第二步
        3. 第三步
        """;

    MarkdownValidator.ValidationResult result = validator.validate(markdown);

    assertInstanceOf(MarkdownValidator.ValidationResult.Valid.class, result);
  }

  @Test
  @DisplayName("使用 * 而非 - 的無序列表應通過（CommonMark 允許）")
  void asteriskList_shouldPass() {
    String markdown =
        """
        * 項目一
        * 項目二
        * 項目三
        """;

    MarkdownValidator.ValidationResult result = validator.validate(markdown);

    assertInstanceOf(MarkdownValidator.ValidationResult.Valid.class, result);
  }
}
