package ltdjms.discord.aichat.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import ltdjms.discord.aichat.domain.AIChatRequest;
import ltdjms.discord.aichat.domain.AIChatResponse;
import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.aichat.services.AIChatService;
import ltdjms.discord.aichat.services.AIClient;
import ltdjms.discord.aichat.services.DefaultAIChatService;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** 測試 {@link DefaultAIChatService} 的端對端流程。 */
class AIChatServiceTest {

  @Test
  void testGenerateResponse_success_shouldSendToDiscord() {
    // Given
    AIServiceConfig config =
        new AIServiceConfig("https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 30);

    AIChatResponse mockResponse =
        new AIChatResponse(
            "chatcmpl-123",
            "chat.completion",
            1677652288L,
            "gpt-3.5-turbo",
            List.of(
                new AIChatResponse.Choice(
                    0, new AIChatResponse.Choice.AIMessage("assistant", "AI 回應"), "stop")),
            new AIChatResponse.Usage(10, 20, 30));

    AIClient mockClient = mock(AIClient.class);
    when(mockClient.sendChatRequest(any(AIChatRequest.class))).thenReturn(Result.ok(mockResponse));

    AIChatService service = new DefaultAIChatService(config, mockClient, null);

    // When
    Result<List<String>, DomainError> result =
        service.generateResponse("channel123", "user456", "測試訊息");

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue()).containsExactly("AI 回應");
    verify(mockClient).sendChatRequest(any(AIChatRequest.class));
  }

  @Test
  void testGenerateResponse_withEmptyMessage_shouldUseDefaultGreeting() {
    // Given
    AIServiceConfig config =
        new AIServiceConfig("https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 30);

    AIChatResponse mockResponse =
        new AIChatResponse(
            "chatcmpl-123",
            "chat.completion",
            1677652288L,
            "gpt-3.5-turbo",
            List.of(
                new AIChatResponse.Choice(
                    0, new AIChatResponse.Choice.AIMessage("assistant", "你好！"), "stop")),
            new AIChatResponse.Usage(10, 20, 30));

    AIClient mockClient = mock(AIClient.class);
    when(mockClient.sendChatRequest(any(AIChatRequest.class))).thenReturn(Result.ok(mockResponse));

    AIChatService service = new DefaultAIChatService(config, mockClient, null);

    // When
    Result<List<String>, DomainError> result =
        service.generateResponse("channel123", "user456", "");

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue()).containsExactly("你好！");
    verify(mockClient).sendChatRequest(any(AIChatRequest.class));
  }

  @Test
  void testGenerateResponse_aiClientError_shouldPropagateError() {
    // Given
    AIServiceConfig config =
        new AIServiceConfig("https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 30);

    AIClient mockClient = mock(AIClient.class);
    when(mockClient.sendChatRequest(any(AIChatRequest.class)))
        .thenReturn(Result.err(DomainError.unexpectedFailure("AI service unavailable", null)));

    AIChatService service = new DefaultAIChatService(config, mockClient, null);

    // When
    Result<List<String>, DomainError> result =
        service.generateResponse("channel123", "user456", "測試訊息");

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.UNEXPECTED_FAILURE);
  }

  @Test
  void testGenerateResponse_withLongResponse_shouldSplitMessages() {
    // Given
    AIServiceConfig config =
        new AIServiceConfig("https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 30);

    // Create a response longer than 2000 characters
    StringBuilder longContent = new StringBuilder();
    for (int i = 0; i < 2500; i++) {
      longContent.append("a");
    }

    AIChatResponse mockResponse =
        new AIChatResponse(
            "chatcmpl-123",
            "chat.completion",
            1677652288L,
            "gpt-3.5-turbo",
            List.of(
                new AIChatResponse.Choice(
                    0,
                    new AIChatResponse.Choice.AIMessage("assistant", longContent.toString()),
                    "stop")),
            new AIChatResponse.Usage(10, 20, 30));

    AIClient mockClient = mock(AIClient.class);
    when(mockClient.sendChatRequest(any(AIChatRequest.class))).thenReturn(Result.ok(mockResponse));

    AIChatService service = new DefaultAIChatService(config, mockClient, null);

    // When
    Result<List<String>, DomainError> result =
        service.generateResponse("channel123", "user456", "測試訊息");

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().size()).isGreaterThan(1);
  }

  @Test
  void testGenerateResponse_withEmptyResponse_shouldReturnError() {
    // Given
    AIServiceConfig config =
        new AIServiceConfig("https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 30);

    AIChatResponse mockResponse =
        new AIChatResponse(
            "chatcmpl-123",
            "chat.completion",
            1677652288L,
            "gpt-3.5-turbo",
            List.of(),
            new AIChatResponse.Usage(10, 20, 30));

    AIClient mockClient = mock(AIClient.class);
    when(mockClient.sendChatRequest(any(AIChatRequest.class))).thenReturn(Result.ok(mockResponse));

    AIChatService service = new DefaultAIChatService(config, mockClient, null);

    // When
    Result<List<String>, DomainError> result =
        service.generateResponse("channel123", "user456", "測試訊息");

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.AI_RESPONSE_EMPTY);
  }
}
