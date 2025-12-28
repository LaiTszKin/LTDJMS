package ltdjms.discord.aichat.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import ltdjms.discord.aichat.domain.AIChatRequest;
import ltdjms.discord.aichat.domain.AIChatResponse;
import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.aichat.services.AIClient;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** 測試 {@link AIClient} 的 HTTP 請求功能。 */
class AIClientTest {

  @Test
  void testSendChatRequest_success_shouldReturnResponse() throws Exception {
    // Given
    AIServiceConfig config =
        new AIServiceConfig(
            "https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body())
        .thenReturn(
            """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-3.5-turbo",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "測試回應"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 20,
                "total_tokens": 30
              }
            }
            """);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    AIClient client = new AIClient(config, mockHttpClient);
    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().getContent()).isEqualTo("測試回應");
  }

  @Test
  void testSendChatRequest_http401_shouldReturnAuthError() throws Exception {
    // Given
    AIServiceConfig config =
        new AIServiceConfig(
            "https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(401);
    when(mockResponse.body()).thenReturn("{\"error\": {\"message\": \"Invalid API key\"}}");

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    AIClient client = new AIClient(config, mockHttpClient);
    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.AI_SERVICE_AUTH_FAILED);
  }

  @Test
  void testSendChatRequest_http429_shouldReturnRateLimitedError() throws Exception {
    // Given
    AIServiceConfig config =
        new AIServiceConfig(
            "https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(429);
    when(mockResponse.body()).thenReturn("{\"error\": {\"message\": \"Rate limit exceeded\"}}");

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    AIClient client = new AIClient(config, mockHttpClient);
    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category())
        .isEqualTo(DomainError.Category.AI_SERVICE_RATE_LIMITED);
  }

  @Test
  void testSendChatRequest_http500_shouldReturnUnavailableError() throws Exception {
    // Given
    AIServiceConfig config =
        new AIServiceConfig(
            "https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.body()).thenReturn("{\"error\": {\"message\": \"Internal server error\"}}");

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    AIClient client = new AIClient(config, mockHttpClient);
    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.AI_SERVICE_UNAVAILABLE);
  }

  @Test
  void testSendChatRequest_timeout_shouldReturnTimeoutError() throws Exception {
    // Given
    AIServiceConfig config =
        new AIServiceConfig(
            "https://api.openai.com/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    HttpClient mockHttpClient = mock(HttpClient.class);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new java.net.http.HttpTimeoutException("timeout"));

    AIClient client = new AIClient(config, mockHttpClient);
    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
    assertThat(result.getError().category()).isEqualTo(DomainError.Category.AI_SERVICE_TIMEOUT);
  }
}
