# Research: AI Chat Mentions

**Feature**: AI Chat Mentions (003-ai-chat)
**Date**: 2025-12-28
**Status**: Completed

## Overview

本文件記錄 AI Chat 功能的技術研究結果，包括 HTTP 客戶端選擇、JSON 序列化庫選擇、AI 服務整合最佳實踐等。

---

## Decision 1: HTTP Client 選擇

### 選擇：Java 17 HttpClient (內建)

### 理由

1. **零依賴成本**：Java 17 內建 `java.net.http.HttpClient`，無需新增外部依賴
2. **現代化 API**：支援同步/非同步請求、HTTP/2、WebSocket
3. **效能優異**：原生實作，效能相當於 OkHttp
4. **維護簡單**：隨 JDK 更新，無需額外維護第三方庫版本
5. **與現有技術棧一致**：專案使用 Java 17，充分利用內建功能

### 替代方案考慮

| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| **OkHttp** | 成熟穩定、連線池管理完善、攔截器強大 | 需要新增依賴、增加維護成本 | 不選擇 - 內建 HttpClient 已足夠 |
| **Apache HttpClient** | 功能豐富、靈活性高 | API 較複雜、版本較舊 | 不選擇 - API 過於複雜 |

### 實作範例

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIClient {
    private final HttpClient httpClient;
    private final AIServiceConfig config;

    public AIClient(AIServiceConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
            .build();
    }

    public Result<String, DomainError> sendRequest(String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey())
            .timeout(Duration.ofSeconds(config.timeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                return Result.ok(response.body());
            } else {
                return Result.err(DomainError.unexpectedFailure(
                    "AI service returned status " + response.statusCode(),
                    null
                ));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.err(DomainError.unexpectedFailure(
                "AI service request interrupted",
                e
            ));
        } catch (Exception e) {
            return Result.err(DomainError.unexpectedFailure(
                "AI service request failed",
                e
            ));
        }
    }
}
```

---

## Decision 2: JSON 序列化庫選擇

### 選擇：Jackson (jackson-databind)

### 理由

1. **業界標準**：Spring Boot、JAX-RS 等框架預設使用 Jackson
2. **功能完整**：支援註解、泛型、自訂序列化器
3. **效能優異**：比 Gson 快約 20-30%
4. **生態成熟**：豐富的模組（如 Jackson Kotlin、Jackson Afterburner）
5. **與 JDA 相容**：JDA 內部使用 Jackson，避免引入兩個 JSON 庫

### 替代方案考慮

| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| **Gson** | API 簡單、輕量級 | 效能較差、功能較少 | 不選擇 - Jackson 更優秀 |
| **JSON-B** | 標準化 API | 需要額外實作 | 不選擇 - 生態不成熟 |

### 實作範例

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// 請求模型
public record AIChatRequest(
    @JsonProperty("model") String model,
    @JsonProperty("messages") List<AIMessage> messages,
    @JsonProperty("temperature") Double temperature,
    @JsonProperty("max_tokens") Integer maxTokens
) {
    public record AIMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
    ) {}

    public static AIChatRequest createUserMessage(
        String content,
        AIServiceConfig config
    ) {
        return new AIChatRequest(
            config.model(),
            List.of(new AIMessage("user", content)),
            config.temperature(),
            config.maxTokens()
        );
    }
}

// 回應模型
public record AIChatResponse(
    @JsonProperty("id") String id,
    @JsonProperty("object") String object,
    @JsonProperty("created") Long created,
    @JsonProperty("model") String model,
    @JsonProperty("choices") List<Choice> choices,
    @JsonProperty("usage") Usage usage
) {
    public record Choice(
        @JsonProperty("index") Integer index,
        @JsonProperty("message") AIMessage message,
        @JsonProperty("finish_reason") String finishReason
    ) {}

    public record AIMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
    ) {}

    public record Usage(
        @JsonProperty("prompt_tokens") Integer promptTokens,
        @JsonProperty("completion_tokens") Integer completionTokens,
        @JsonProperty("total_tokens") Integer totalTokens
    ) {}

    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        return choices.get(0).message().content();
    }
}

// ObjectMapper 配置
public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(json, clazz);
    }
}
```

---

## Decision 3: AI 服務 API 相容性

### 選擇：OpenAI Chat Completions API 標準

### 理由

1. **業界標準**：OpenAI API 成為 LLM 服務的事實標準
2. **廣泛相容**：多個 AI 服務供應商提供相容端點：
   - OpenAI (GPT-3.5/4)
   - Azure OpenAI
   - Anthropic Claude (透過相容層)
   - 開源模型 (Ollama、LocalAI)
   - 各國雲端服務商 (百度文心、阿里通義千問等)

### API 端點格式

```
POST {BASE_URL}/chat/completions
Headers:
  Content-Type: application/json
  Authorization: Bearer {API_KEY}
Body:
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {"role": "user", "content": "你的訊息"}
  ],
  "temperature": 0.7,
  "max_tokens": 500
}
```

### 設計原則

1. **無狀態**：每次請求獨立，不保存上下文
2. **單輪對話**：僅發送使用者訊息，不包含系統提示詞
3. **可配置**：所有參數透過 .env 設定

---

## Decision 4: Discord 訊息分割策略

### 選擇：智慧分割（保留段落完整性）

### 理由

Discord 單則訊息長度限制為 **2000 字元**。當 AI 回應超過此限制時，需要分割為多則訊息。

### 分割策略

1. **字元限制**：每則訊息最多 2000 字元（預留 20 字元緩衝 = 1980 字元）
2. **段落優先**：在換行符號 `\n` 處分割
3. **句子優先**：若無段落，在句號 `。`、`！`、`？` 處分割
4. **強制分割**：若無合適分割點，則在 1980 字元處強制分割

### 實作範例

```java
import java.util.ArrayList;
import java.util.List;

public class MessageSplitter {
    private static final int MAX_MESSAGE_LENGTH = 1980; // 預留 20 字元緩衝

    public static List<String> split(String content) {
        if (content.length() <= MAX_MESSAGE_LENGTH) {
            return List.of(content);
        }

        List<String> messages = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + MAX_MESSAGE_LENGTH, content.length());

            // 優先在段落處分割
            int lastNewline = content.lastIndexOf('\n', end);
            if (lastNewline > start) {
                end = lastNewline + 1;
            } else {
                // 其次在句子處分割
                int lastSentence = findLastSentenceBoundary(content, start, end);
                if (lastSentence > start) {
                    end = lastSentence + 1;
                }
            }

            messages.add(content.substring(start, end).trim());
            start = end;
        }

        return messages;
    }

    private static int findLastSentenceBoundary(String content, int start, int end) {
        String boundaries = "。！？";
        int last = -1;

        for (char c : boundaries.toCharArray()) {
            int pos = content.lastIndexOf(c, end - 1);
            if (pos > start && pos > last) {
                last = pos;
            }
        }

        return last;
    }
}
```

---

## Decision 5: 錯誤處理策略

### 選擇：擴展 DomainError 類型

### 新增錯誤類型

```java
// 在 DomainError.Category 中新增
public enum Category {
    // ... 現有類型
    AI_SERVICE_TIMEOUT,           // AI 服務連線逾時
    AI_SERVICE_AUTH_FAILED,       // AI 服務認證失敗
    AI_SERVICE_RATE_LIMITED,      // AI 服務速率限制
    AI_SERVICE_UNAVAILABLE,       // AI 服務不可用
    AI_RESPONSE_EMPTY,            // AI 回應為空
    AI_RESPONSE_INVALID,          // AI 回應格式無效
}
```

### 錯誤對應表

| HTTP 狀態碼 | DomainError 類型 | 使用者訊息 |
|------------|-----------------|-----------|
| 401 Unauthorized | AI_SERVICE_AUTH_FAILED | AI 服務認證失敗，請聯絡管理員 |
| 429 Too Many Requests | AI_SERVICE_RATE_LIMITED | AI 服務暫時忙碌，請稍後再試 |
| 500+ Server Error | AI_SERVICE_UNAVAILABLE | AI 服務暫時無法使用 |
| 逾時 | AI_SERVICE_TIMEOUT | AI 服務連線逾時，請稍後再試 |
| 空回應 | AI_RESPONSE_EMPTY | AI 沒有產生回應 |
| JSON 解析失敗 | AI_RESPONSE_INVALID | AI 回應格式錯誤 |

---

## Decision 6: 配置管理策略

### 選擇：擴展 EnvironmentConfig 與 .env

### 新增環境變數

```bash
# AI 服務配置
AI_SERVICE_BASE_URL=https://api.openai.com/v1
AI_SERVICE_API_KEY=your_api_key_here
AI_SERVICE_MODEL=gpt-3.5-turbo
AI_SERVICE_TEMPERATURE=0.7
AI_SERVICE_MAX_TOKENS=500
AI_SERVICE_TIMEOUT_SECONDS=30
```

### 配置類別設計

```java
package ltdjms.discord.aichat.domain;

import java.time.Duration;

/**
 * AI 服務配置
 *
 * @param baseUrl          AI 服務 Base URL (例如: https://api.openai.com/v1)
 * @param apiKey           API 金鑰
 * @param model            模型名稱 (例如: gpt-3.5-turbo)
 * @param temperature      溫度 (0.0-2.0，控制回應隨機性)
 * @param maxTokens        最大 Token 數
 * @param timeoutSeconds   連線逾時秒數（不限制推理時間）
 */
public record AIServiceConfig(
    String baseUrl,
    String apiKey,
    String model,
    double temperature,
    int maxTokens,
    int timeoutSeconds
) {
    /**
     * 從 EnvironmentConfig 建立配置
     */
    public static AIServiceConfig from(EnvironmentConfig env) {
        return new AIServiceConfig(
            env.getOrDefault("AI_SERVICE_BASE_URL", "https://api.openai.com/v1"),
            env.getRequired("AI_SERVICE_API_KEY"),
            env.getOrDefault("AI_SERVICE_MODEL", "gpt-3.5-turbo"),
            env.getDoubleOrDefault("AI_SERVICE_TEMPERATURE", 0.7),
            env.getIntOrDefault("AI_SERVICE_MAX_TOKENS", 500),
            env.getIntOrDefault("AI_SERVICE_TIMEOUT_SECONDS", 30)
        );
    }

    /**
     * 驗證配置
     */
    public Result<Unit, DomainError> validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_BASE_URL cannot be empty"));
        }
        if (apiKey == null || apiKey.isBlank()) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_API_KEY cannot be empty"));
        }
        if (model == null || model.isBlank()) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_MODEL cannot be empty"));
        }
        if (temperature < 0.0 || temperature > 2.0) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_TEMPERATURE must be between 0.0 and 2.0"));
        }
        if (maxTokens < 1 || maxTokens > 4096) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_MAX_TOKENS must be between 1 and 4096"));
        }
        if (timeoutSeconds < 1 || timeoutSeconds > 120) {
            return Result.err(DomainError.invalidInput("AI_SERVICE_TIMEOUT_SECONDS must be between 1 and 120"));
        }
        return Result.okVoid();
    }
}
```

---

## Decision 7: JDA 事件監聽策略

### 選擇：使用 GenericEventMonitor 攔截 MessageReceivedEvent

### 理由

1. **現有模式**：專案已使用 `GenericEventMonitor` 進行事件監聽
2. **提及檢測**：檢查訊息是否提及機器人
3. **非同步處理**：避免阻塞 JDA 事件執行緒

### 實作範例

```java
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AIChatMentionListener extends ListenerAdapter {
    private final AIChatService aiChatService;
    private final JDA jda;

    @Inject
    public AIChatMentionListener(AIChatService aiChatService, JDA jda) {
        this.aiChatService = aiChatService;
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 忽略機器人自己的訊息
        if (event.getAuthor().isBot()) {
            return;
        }

        // 忽略私人訊息
        if (!event.isFromGuild()) {
            return;
        }

        // 檢查是否提及機器人
        String message = event.getMessage().getContentRaw();
        String botMention = "<@" + jda.getSelfUser().getId() + ">";

        if (!message.contains(botMention)) {
            return;
        }

        // 移除提及部分，提取使用者訊息
        String userMessage = message.replace(botMention, "").trim();

        // 如果訊息為空，發送預設問候
        if (userMessage.isBlank()) {
            userMessage = "你好";
        }

        // 非同步處理 AI 回應
        event.getGuild().retrieveOwner().queue(owner -> {
            aiChatService.generateResponse(
                event.getChannel().getId(),
                event.getAuthor().getId(),
                userMessage
            ).handle(result -> {
                if (result.isOk()) {
                    // 成功：發送回應（已在 service 中處理）
                } else {
                    // 失敗：發送錯誤訊息
                    event.getChannel().sendMessage(
                        ":warning: " + result.getError().userMessage()
                    ).queue();
                }
            });
        });
    }
}
```

---

## Decision 8: 日誌記錄策略

### 選擇：結構化日誌 + MDC

### 日誌等級

| 等級 | 用途 |
|------|------|
| ERROR | AI 服務呼叫失敗、認證錯誤 |
| WARN | 速率限制、連線逾時、空回應 |
| INFO | AI 請求成功、回應時間 |
| DEBUG | 詳細請求/回應內容 |

### 日誌格式

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AIClient {
    private static final Logger logger = LoggerFactory.getLogger(AIClient.class);

    public Result<String, DomainError> sendRequest(String jsonBody) {
        long startTime = System.currentTimeMillis();

        MDC.put("guild_id", guildId);
        MDC.put("user_id", userId);
        MDC.put("model", config.model());

        try {
            // ... 發送請求

            long duration = System.currentTimeMillis() - startTime;

            if (response.statusCode() == 200) {
                logger.info(
                    "AI request completed: status={}, duration_ms={}, tokens={}",
                    response.statusCode(),
                    duration,
                    parseTokenUsage(response.body())
                );
            } else {
                logger.warn(
                    "AI request failed: status={}, duration_ms={}, body={}",
                    response.statusCode(),
                    duration,
                    response.body()
                );
            }

        } catch (Exception e) {
            logger.error("AI request exception", e);
        } finally {
            MDC.clear();
        }
    }
}
```

---

## Decision 9: 測試策略

### 單元測試

1. **AIClientTest**：測試 HTTP 請求、JSON 序列化
   - Mock HttpClient 回應
   - 測試各種 HTTP 狀態碼
   - 測試連線逾時處理

2. **AIChatServiceTest**：測試業務邏輯
   - 測試訊息分割
   - 測試錯誤轉換
   - 測試 Discord 訊息發送

3. **MessageSplitterTest**：測試訊息分割
   - 測試短訊息（不分割）
   - 測試段落分割
   - 測試句子分割
   - 測試強制分割

### 整合測試

1. **AIChatIntegrationTest**：測試完整流程
   - 使用 Testcontainers 啟動 Mock AI 服務（如 Wiremock）
   - 測試端對端流程
   - 測試並行請求

---

## Decision 10: 依賴注入策略

### 選擇：新增 AIChatModule

### 模組設計

```java
package ltdjms.discord.shared.di;

import dagger.Module;
import dagger.Provides;
import ltdjms.discord.aichat.domain.AIServiceConfig;
import ltdjms.discord.aichat.services.AIClient;
import ltdjms.discord.aichat.services.AIChatService;
import ltdjms.discord.aichat.services.DefaultAIChatService;
import ltdjms.discord.shared.EnvironmentConfig;

import javax.inject.Singleton;

@Module
public class AIChatModule {
    @Provides
    @Singleton
    public AIServiceConfig provideAIServiceConfig(EnvironmentConfig envConfig) {
        AIServiceConfig config = AIServiceConfig.from(envConfig);
        Result<Unit, DomainError> validation = config.validate();
        if (validation.isErr()) {
            throw new IllegalStateException(
                "Invalid AI service config: " + validation.getError().message()
            );
        }
        return config;
    }

    @Provides
    @Singleton
    public AIClient provideAIClient(AIServiceConfig config) {
        return new AIClient(config);
    }

    @Provides
    @Singleton
    public AIChatService provideAIChatService(AIClient aiClient) {
        return new DefaultAIChatService(aiClient);
    }
}
```

---

## Maven 依賴總結

### 需要新增的依賴

```xml
<!-- Jackson JSON 序列化 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>

<!-- Jackson Core (已透過 JDA 引入，但需明確版本) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.17.2</version>
</dependency>

<!-- Jackson Annotations (用於 @JsonProperty) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.17.2</version>
</dependency>
```

### 版本選擇

- **Jackson 2.17.2**：最新穩定版本（2024-06）
- 與 JDA 5.2.2 使用的 Jackson 版本相容

---

## 技術風險評估

| 風險 | 影響 | 緩解策略 |
|------|------|---------|
| AI 服務供應商變更 API 格式 | 高 | 使用相容性標準，提供配置靈活性 |
| AI 回應超過 Discord 長度限制 | 中 | 實作智慧分割策略 |
| AI 服務連線逾時導致 Discord 逾時 | 中 | 設定適當的連線逾時時間，記錄警告 |
| 並行請求導致速率限制 | 低 | 記錄速率限制錯誤，友善提示使用者 |

---

## 未來擴展方向

1. **串流式回應**：支援 SSE (Server-Sent Events) 串流式 AI 回應
2. **上下文記憶**：可選的對話歷史保存
3. **多服務支援**：支援多個 AI 服務供應商
4. **速率限制**：每使用者請求頻率限制
5. **系統提示詞**：可配置的 AI 人格設定

---

## 結論

本研究已完成以下決策：

1. ✅ HTTP Client：Java 17 內建 HttpClient
2. ✅ JSON 序列化：Jackson 2.17.2
3. ✅ AI 服務：OpenAI Chat Completions API 標準
4. ✅ 訊息分割：智慧分割（保留段落完整性）
5. ✅ 錯誤處理：擴展 DomainError
6. ✅ 配置管理：.env + EnvironmentConfig
7. ✅ 事件監聽：JDA GenericEventMonitor
8. ✅ 日誌記錄：結構化日誌 + MDC
9. ✅ 測試策略：單元測試 + 整合測試
10. ✅ 依賴注入：Dagger 2 AIChatModule

所有決策都符合 LTDJMS 專案的憲法要求，並與現有架構保持一致。
