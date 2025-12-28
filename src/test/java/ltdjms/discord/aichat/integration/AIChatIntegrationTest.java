package ltdjms.discord.aichat.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import ltdjms.discord.aichat.domain.AIChatRequest;
import ltdjms.discord.aichat.domain.AIChatResponse;
import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.aichat.services.AIClient;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** 整合測試，使用 Wiremock 模擬 AI 服務端點。 */
class AIChatIntegrationTest {

  private WireMockServer wireMockServer;
  private AIClient client;
  private AIServiceConfig config;

  @BeforeEach
  void setUp() {
    // Start Wiremock server
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
    wireMockServer.start();

    // Create config pointing to Wiremock server
    config =
        new AIServiceConfig(
            "http://localhost:8089/v1", "test-api-key", "gpt-3.5-turbo", 0.7, 500, 30);

    client = new AIClient(config);
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  void testFullChatFlow_success_shouldReturnResponse() {
    // Given
    wireMockServer.stubFor(
        WireMock.post(WireMock.urlEqualTo("/v1/chat/completions"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
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
                                "content": "整合測試回應"
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
                        """)));

    AIChatRequest request = AIChatRequest.createUserMessage("整合測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getValue().getContent()).isEqualTo("整合測試回應");

    // Verify request was made
    wireMockServer.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/v1/chat/completions"))
            .withHeader("Content-Type", WireMock.containing("application/json"))
            .withHeader("Authorization", WireMock.containing("Bearer test-api-key")));
  }

  @Test
  void testChatFlow_authError_shouldReturn401() {
    // Given
    wireMockServer.stubFor(
        WireMock.post(WireMock.urlEqualTo("/v1/chat/completions"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "error": {
                            "message": "Invalid API key",
                            "type": "invalid_request_error",
                            "code": "invalid_api_key"
                          }
                        }
                        """)));

    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
  }

  @Test
  void testChatFlow_rateLimitError_shouldReturn429() {
    // Given
    wireMockServer.stubFor(
        WireMock.post(WireMock.urlEqualTo("/v1/chat/completions"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "error": {
                            "message": "Rate limit exceeded",
                            "type": "rate_limit_error"
                          }
                        }
                        """)));

    AIChatRequest request = AIChatRequest.createUserMessage("測試訊息", config);

    // When
    Result<AIChatResponse, DomainError> result = client.sendChatRequest(request);

    // Then
    assertThat(result.isErr()).isTrue();
  }
}
