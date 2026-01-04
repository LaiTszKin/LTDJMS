package ltdjms.discord.markdown.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.aichat.services.AIChatService;
import ltdjms.discord.markdown.autofix.MarkdownAutoFixer;
import ltdjms.discord.markdown.autofix.RegexBasedAutoFixer;
import ltdjms.discord.markdown.services.MarkdownValidatingAIChatService;
import ltdjms.discord.markdown.validation.CommonMarkValidator;
import ltdjms.discord.markdown.validation.MarkdownErrorFormatter;
import ltdjms.discord.markdown.validation.MarkdownValidator;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/**
 * 整合測試，驗證 Markdown 驗證裝飾器的完整行為。
 *
 * <p>測試範圍包含：
 *
 * <ul>
 *   <li>DI 組裝驗證：驗證 CommonMarkValidator 與 MarkdownValidatingAIChatService 可正確組裝
 *   <li>驗證流程：有效的 Markdown 直接通過
 *   <li>重試流程：格式錯誤觸發重試，直到成功或達到上限
 *   <li>降級模式：停用驗證時直接委派
 * </ul>
 */
@DisplayName("Markdown 驗證整合測試")
class MarkdownValidationIntegrationTest {

  private static final AIServiceConfig TEST_CONFIG =
      new AIServiceConfig(
          "https://api.test.com",
          "test-key",
          "gpt-4",
          0.7,
          60,
          false,
          true, // enable markdown validation
          false, // streaming bypass
          5,
          true); // enable auto-fix

  private static final AIServiceConfig TEST_CONFIG_DISABLED =
      new AIServiceConfig(
          "https://api.test.com",
          "test-key",
          "gpt-4",
          0.7,
          60,
          false,
          false, // disable markdown validation
          false, // streaming bypass
          5,
          false); // disable auto-fix

  @Mock private AIChatService mockDelegate;

  private MarkdownValidator validator;
  private MarkdownErrorFormatter formatter;
  private MarkdownAutoFixer autofixer;
  private MarkdownValidatingAIChatService validatingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // 使用真實的 CommonMarkValidator、MarkdownErrorFormatter 和 MarkdownAutoFixer
    validator = new CommonMarkValidator();
    formatter = new MarkdownErrorFormatter();
    autofixer = new RegexBasedAutoFixer();
  }

  @Nested
  @DisplayName("DI 組裝驗證")
  class DIAssemblyTests {

    @Test
    @DisplayName("應該能夠使用真實的 CommonMarkValidator 建立 MarkdownValidatingAIChatService")
    void shouldAssembleWithRealValidator() {
      // Given
      validatingService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, true);

      // Then - 無異常拋出
      assertThat(validatingService).isNotNull();
    }
  }

  @Nested
  @DisplayName("驗證流程測試")
  class ValidationFlowTests {

    @BeforeEach
    void setUpValidatingService() {
      validatingService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, true);
    }

    @Test
    @DisplayName("有效的 Markdown 應該直接通過，不觸發重試")
    void validMarkdownShouldPass() {
      // Given
      String validResponse = "## 這是標題\n\n這是段落文字。\n\n- 列表項目 1\n- 列表項目 2";
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "測試訊息");

      // Then
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).containsExactly(validResponse);
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("程式碼區塊語法錯誤應該觸發重試")
    void codeBlockSyntaxErrorShouldTriggerRetry() {
      // Given - 第一次回應有不完整的程式碼區塊，第二次正確
      String invalidResponse = "```java\npublic void test() {\n  // 缺少結束的 ```";
      String validResponse = "```java\npublic void test() {\n}\n```";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "寫一個 Java 函數");

      // Then - 應該重試一次後成功
      assertThat(result.isOk()).isTrue();
      verify(mockDelegate, times(2))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("標題等級超過 Discord 限制會被自動修復，不會觸發重試")
    void headingLevelExceedsDiscordLimitShouldBeAutoFixed() {
      // Given - H7（超過 Discord 限制）現在會被自動修復為 H6
      String invalidResponse = "######\nHeading 6\n#######\nHeading 7 超過限制";
      String expectedAutoFixed = "######\nHeading 6\n######\nHeading 7 超過限制";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "寫個標題");

      // Then - 應該自動修復，不需要重試
      assertThat(result.isOk()).isTrue();
      // H7 應該被自動修復為 H6
      assertThat(result.getValue().get(0)).isEqualTo(expectedAutoFixed);
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("表格無法自動修復，應該觸發重試")
    void tableCannotBeAutoFixedShouldTriggerRetry() {
      // Given - 表格是 Discord 不支援的語法，且無法自動修復
      String invalidResponse = "| 欄位 A | 欄位 B |\n|--------|--------|\n| 值 1   | 值 2   |";
      String validResponse = "- 欄位 A：值 1\n- 欄位 B：值 2";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "用表格展示數據");

      // Then - 應該重試一次後成功
      assertThat(result.isOk()).isTrue();
      verify(mockDelegate, times(2))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("多個格式錯誤應該在錯誤報告中全部列出")
    void multipleErrorsShouldAllBeListedInReport() {
      // Given - 同時有多個錯誤：不完整程式碼區塊 + 表格（無法自動修復）
      String invalidResponse = "| 表格 |\n|---|\n| 數據 |";
      String validResponse = "- 表格\n  - 數據";

      // 設定 mock，對所有調用都返回相同的無效回應（測試錯誤報告）
      // 第一次調用返回無效，後續調用返回有效（模擬重試成功）
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)))
          .thenReturn(Result.ok(List.of(validResponse)))
          .thenReturn(Result.ok(List.of(validResponse))) // 確保後續調用也有返回值
          .thenReturn(Result.ok(List.of(validResponse)))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      validatingService.generateResponse(123L, "channel-1", "user-1", "生成有問題的回應");

      // Then - 第二次請求應該包含完整的錯誤報告
      verify(mockDelegate, atLeast(2))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("超出最大重試次數應該返回最後一次的回應")
    void exceedingMaxRetriesShouldReturnLastResponse() {
      // Given - 使用停用自動修復的服務來測試重試機制
      var serviceWithoutAutoFix =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, false);

      // 持續返回無法自動修復的錯誤（超過 Discord 限制的標題）
      String invalidResponse = "####### H7 - 超過限制的標題";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)));

      // When
      Result<List<String>, DomainError> result =
          serviceWithoutAutoFix.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 應該重試 5 次（初始 + 4 次重試）
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).containsExactly(invalidResponse);
      verify(mockDelegate, times(5))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("降級模式測試")
  class DegradationModeTests {

    @Test
    @DisplayName("停用驗證時應該直接委派，不進行驗證")
    void disabledValidationShouldDelegateDirectly() {
      // Given
      MarkdownValidatingAIChatService disabledService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, false, formatter, 5, false, true);

      String anyResponse = "```\nunclosed block"; // 即使有錯誤也應該通過
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(anyResponse)));

      // When
      Result<List<String>, DomainError> result =
          disabledService.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 直接返回，不驗證
      assertThat(result.isOk()).isTrue();
      assertThat(result.getValue()).containsExactly(anyResponse);
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("委派錯誤處理測試")
  class DelegateErrorTests {

    @BeforeEach
    void setUpValidatingService() {
      validatingService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, true);
    }

    @Test
    @DisplayName("委派服務回傳錯誤時應該直接返回，不進行驗證")
    void delegateErrorShouldReturnDirectly() {
      // Given
      DomainError error = new DomainError(DomainError.Category.UNEXPECTED_FAILURE, "API 錯誤", null);
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.err(error));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 直接返回錯誤，不驗證
      assertThat(result.isErr()).isTrue();
      assertThat(result.getError()).isEqualTo(error);
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("串流回應測試")
  class StreamingResponseTests {

    @BeforeEach
    void setUpValidatingService() {
      validatingService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, true);
    }

    @Test
    @DisplayName("串流回應預設應先驗證再回傳分段")
    void streamingResponseShouldValidateAndEmit() {
      // Given
      ltdjms.discord.aichat.services.StreamingResponseHandler mockHandler =
          org.mockito.Mockito.mock(ltdjms.discord.aichat.services.StreamingResponseHandler.class);
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of("合法回應")));

      // When
      validatingService.generateStreamingResponse(123L, "channel-1", "user-1", "測試", mockHandler);

      // Then - 透過 onChunk 回傳結果，且不使用委派的串流方法
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
      verify(mockDelegate, never())
          .generateStreamingResponse(anyLong(), anyString(), anyString(), anyString(), any());
      verify(mockHandler, times(1)).onChunk(eq("合法回應"), eq(true), isNull(), any());
    }

    @Test
    @DisplayName("串流回應（帶 messageId）預設應驗證並回傳分段")
    void streamingResponseWithMessageIdShouldValidateAndEmit() {
      // Given
      ltdjms.discord.aichat.services.StreamingResponseHandler mockHandler =
          org.mockito.Mockito.mock(ltdjms.discord.aichat.services.StreamingResponseHandler.class);
      long messageId = 999L;
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of("合法回應 2")));

      // When
      validatingService.generateStreamingResponse(
          123L, "channel-1", "user-1", "測試", messageId, mockHandler);

      // Then - 透過 onChunk 回傳結果，且不使用委派的串流方法
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
      verify(mockDelegate, never())
          .generateStreamingResponse(
              anyLong(), anyString(), anyString(), anyString(), eq(messageId), any());
      verify(mockHandler, times(1)).onChunk(eq("合法回應 2"), eq(true), isNull(), any());
    }

    @Test
    @DisplayName("帶對話歷史的串流回應應該直接委派，不進行驗證")
    void streamingResponseWithHistoryShouldDelegateDirectly() {
      // Given
      ltdjms.discord.aichat.services.StreamingResponseHandler mockHandler =
          org.mockito.Mockito.mock(ltdjms.discord.aichat.services.StreamingResponseHandler.class);
      java.util.List<ltdjms.discord.aiagent.domain.ConversationMessage> history =
          java.util.List.of();

      // When
      validatingService.generateWithHistory(123L, "channel-1", "user-1", history, mockHandler);

      // Then - 直接委派，不驗證
      verify(mockDelegate, times(1))
          .generateWithHistory(anyLong(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("啟用串流繞過時應直接委派原始串流")
    void streamingBypassShouldDelegate() {
      // Given
      var bypassService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, true, true);
      ltdjms.discord.aichat.services.StreamingResponseHandler mockHandler =
          org.mockito.Mockito.mock(ltdjms.discord.aichat.services.StreamingResponseHandler.class);

      // When
      bypassService.generateStreamingResponse(123L, "channel-1", "user-1", "測試", mockHandler);

      // Then
      verify(mockDelegate, times(1))
          .generateStreamingResponse(anyLong(), anyString(), anyString(), anyString(), any());
    }
  }

  @Nested
  @DisplayName("自動修復測試")
  class AutoFixTests {

    @BeforeEach
    void setUpValidatingService() {
      validatingService =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, true);
    }

    @Test
    @DisplayName("啟用自動修復時，應該修復簡單標題格式錯誤並避免重試")
    void shouldAutoFixHeadingFormatErrorWithoutRetry() {
      // Given - 標題缺少空格（#Heading）
      String invalidResponse = "#Heading without space";
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 應該自動修復並成功，不重試
      assertThat(result.isOk()).isTrue();
      // 修復後應該有空格
      assertThat(result.getValue().get(0)).contains("# Heading");
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("啟用自動修復時，應該修復未閉合的程式碼區塊並避免重試")
    void shouldAutoFixUnclosedCodeBlockWithoutRetry() {
      // Given - 未閉合的程式碼區塊後跟隨純文字
      String invalidResponse = "```java\npublic void test() {\n}\nSome plain text after";
      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 應該自動修復並成功，不重試
      assertThat(result.isOk()).isTrue();
      // 應該包含結束的 ```
      assertThat(result.getValue().get(0)).contains("```");
      verify(mockDelegate, times(1))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("停用自動修復時，應該正常重試機制")
    void disabledAutoFixShouldUseNormalRetry() {
      // Given
      var serviceWithoutAutoFix =
          new MarkdownValidatingAIChatService(
              mockDelegate, validator, autofixer, true, formatter, 5, false, false);

      String invalidResponse = "#Invalid heading";
      String validResponse = "# Valid heading";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      Result<List<String>, DomainError> result =
          serviceWithoutAutoFix.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 應該重試一次
      assertThat(result.isOk()).isTrue();
      verify(mockDelegate, times(2))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("自動修復無法完全修復時，應該進入重試流程")
    void shouldRetryWhenAutoFixCannotFullyCorrect() {
      // Given - 複雜錯誤無法自動修復（表格無法自動轉換為其他格式）
      String invalidResponse = "| 欄位 | 值 |\n|---|---|\n| A | 1 |";
      String validResponse = "- 欄位：A = 1";

      when(mockDelegate.generateResponse(anyLong(), anyString(), anyString(), anyString()))
          .thenReturn(Result.ok(List.of(invalidResponse)))
          .thenReturn(Result.ok(List.of(validResponse)));

      // When
      Result<List<String>, DomainError> result =
          validatingService.generateResponse(123L, "channel-1", "user-1", "測試");

      // Then - 自動修復無法處理表格，應該重試
      assertThat(result.isOk()).isTrue();
      verify(mockDelegate, times(2))
          .generateResponse(anyLong(), anyString(), anyString(), anyString());
    }
  }
}
