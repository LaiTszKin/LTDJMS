package ltdjms.discord.aichat.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ltdjms.discord.aichat.domain.AIChatRequest;
import ltdjms.discord.aichat.domain.AIChatResponse;
import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.shared.DomainError;
import ltdjms.discord.shared.Result;

/** AI HTTP 客戶端，負責與 AI 服務通訊。 */
public final class AIClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(AIClient.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final HttpClient httpClient;
  private final AIServiceConfig config;

  /**
   * 創建 AIClient。
   *
   * @param config AI 服務配置
   */
  public AIClient(AIServiceConfig config) {
    this.config = config;
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(config.timeoutSeconds())).build();
  }

  /**
   * 創建 AIClient with custom HttpClient (for testing).
   *
   * @param config AI 服務配置
   * @param httpClient 自訂 HttpClient
   */
  public AIClient(AIServiceConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  /**
   * 發送聊天請求到 AI 服務。
   *
   * @param request AI 聊天請求
   * @return AI 回應或錯誤
   */
  public Result<AIChatResponse, DomainError> sendChatRequest(AIChatRequest request) {
    long startTime = System.currentTimeMillis();

    MDC.put("model", config.model());

    try {
      String jsonBody = OBJECT_MAPPER.writeValueAsString(request);

      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(config.baseUrl() + "/chat/completions"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + config.apiKey())
              .timeout(Duration.ofSeconds(config.timeoutSeconds()))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      int statusCode = response.statusCode();
      String responseBody = response.body();
      long duration = System.currentTimeMillis() - startTime;

      if (statusCode == 200) {
        AIChatResponse aiResponse = OBJECT_MAPPER.readValue(responseBody, AIChatResponse.class);

        LOGGER.info(
            "AI request completed: status={}, duration_ms={}, tokens={}",
            statusCode,
            duration,
            aiResponse.usage() != null ? aiResponse.usage().totalTokens() : "N/A");

        return Result.ok(aiResponse);
      }

      LOGGER.warn("AI request failed: status={}, duration_ms={}", statusCode, duration);

      // Map HTTP status codes to DomainError categories
      if (statusCode == 401) {
        return Result.err(
            new DomainError(
                DomainError.Category.AI_SERVICE_AUTH_FAILED,
                "AI service authentication failed: " + responseBody,
                null));
      }
      if (statusCode == 429) {
        return Result.err(
            new DomainError(
                DomainError.Category.AI_SERVICE_RATE_LIMITED,
                "AI service rate limited: " + responseBody,
                null));
      }
      if (statusCode >= 500) {
        return Result.err(
            new DomainError(
                DomainError.Category.AI_SERVICE_UNAVAILABLE,
                "AI service unavailable: " + responseBody,
                null));
      }
      return Result.err(
          new DomainError(
              DomainError.Category.UNEXPECTED_FAILURE,
              "AI service returned status " + statusCode + ": " + responseBody,
              null));

    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize AI request", e);
      return Result.err(
          new DomainError(
              DomainError.Category.AI_RESPONSE_INVALID, "Failed to serialize AI request", e));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error("AI service request interrupted", e);
      return Result.err(
          new DomainError(
              DomainError.Category.AI_SERVICE_TIMEOUT, "AI service request interrupted", e));
    } catch (HttpTimeoutException e) {
      LOGGER.error("AI service request timed out", e);
      return Result.err(
          new DomainError(
              DomainError.Category.AI_SERVICE_TIMEOUT, "AI service request timed out", e));
    } catch (IOException e) {
      LOGGER.error("AI service request failed", e);
      return Result.err(
          new DomainError(
              DomainError.Category.AI_SERVICE_UNAVAILABLE, "AI service request failed", e));
    } finally {
      MDC.clear();
    }
  }
}
