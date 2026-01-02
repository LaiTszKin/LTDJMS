# Markdown 格式驗證功能設計

**日期**: 2026-01-03
**狀態**: 設計已完成
**關聯規格**: markdown-validation

---

## 概述

實作一個 Markdown 格式驗證器，檢查 LLM 回覆是否能正確渲染為結構化 Markdown，並在格式錯誤時自動觸發重新生成。

### 核心需求

1. **雙重驗證**：同時確保 Discord 渲染正確性和 Markdown 語法規範
2. **完成後檢查**：等待完整回應生成後再進行驗證
3. **重試至成功**：驗證失敗時自動重新生成，上限 5 次
4. **結構化錯誤回饋**：將錯誤詳細資訊與原始提示詞一起回傳給 LLM
5. **裝飾器模式**：使用裝飾器模式實作，不修改現有代碼

---

## 架構概覽

### 核心架構

```
Command Handler 層
        ↓ 調用 generateResponse()
MarkdownValidatingAIChatService (裝飾器)
        ↓ 委託調用
LangChain4jAIChatService (被裝飾者)
        ↓
      LLM
```

### 依賴注入結構

- `AIServiceConfig` 新增 `enableMarkdownValidation` 選項
- `AIChatModule` 根據配置決定返回原始服務或裝飾後的服務
- 裝飾器模式確保零侵入性修改現有代碼

---

## 核心元件設計

### MarkdownValidator 介面

```java
public interface MarkdownValidator {
    ValidationResult validate(String markdown);

    sealed interface ValidationResult {
        record Valid(String markdown) implements ValidationResult {}
        record Invalid(List<MarkdownError> errors) implements ValidationResult {}
    }

    record MarkdownError(
        ErrorType type,
        int lineNumber,
        int column,
        String context,
        String suggestion
    ) {}

    enum ErrorType {
        MALFORMED_LIST,
        UNCLOSED_CODE_BLOCK,
        HEADING_LEVEL_EXCEEDED,
        MALFORMED_TABLE,
        ESCAPE_CHARACTER_MISSING,
        DISCORD_RENDER_ISSUE
    }
}
```

### CommonMarkValidator 實作

使用 CommonMark Java 的 `Parser` 和 `HtmlRenderer` 進行驗證：
- 解析階段捕獲語法錯誤
- 渲染階段驗證 HTML 結構完整性
- 額外檢查 Discord 特定限制（表格列寬、程式碼區塊語言標記）

---

## 資料流與重試邏輯

### MarkdownValidatingAIChatService 實作

```java
public final class MarkdownValidatingAIChatService implements AIChatService {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final AIChatService delegate;
    private final MarkdownValidator validator;
    private final boolean enabled;
    private final MarkdownErrorFormatter errorFormatter;

    @Override
    public Result<List<String>, DomainError> generateResponse(
        long guildId, String channelId, String userId, String userMessage) {

        if (!enabled) {
            return delegate.generateResponse(guildId, channelId, userId, userMessage);
        }

        String originalPrompt = userMessage;
        String currentPrompt = userMessage;
        String lastResponse = null;
        int attempt = 0;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++;

            Result<List<String>, DomainError> result =
                delegate.generateResponse(guildId, channelId, userId, currentPrompt);

            if (result.isErr()) {
                return result;
            }

            String fullResponse = String.join("\n", result.getValue());
            lastResponse = fullResponse;

            ValidationResult validation = validator.validate(fullResponse);

            if (validation instanceof ValidationResult.Valid) {
                return result;
            }

            ValidationResult.Invalid invalid = (ValidationResult.Invalid) validation;
            String errorReport = errorFormatter.formatErrorReport(
                originalPrompt, invalid.errors(), attempt, fullResponse);

            currentPrompt = buildRetryPrompt(originalPrompt, errorReport);
            LOG.warn("Markdown validation failed (attempt {}/{}): {} errors",
                attempt, MAX_RETRY_ATTEMPTS, invalid.errors().size());
        }

        LOG.warn("Markdown validation exceeded max attempts, returning last response");
        return Result.ok(List.of(lastResponse));
    }

    private String buildRetryPrompt(String originalPrompt, String errorReport) {
        return String.format("""
            [系統提示：你的上一次回應存在 Markdown 格式錯誤]

            原始用戶訊息：
            %s

            格式驗證錯誤報告：
            %s

            請修正上述格式錯誤並重新生成回應。
            """, originalPrompt, errorReport);
    }
}
```

---

## 錯誤報告格式化器

### MarkdownErrorFormatter 設計

將驗證錯誤格式化為結構化報告，包含：
- 概述區段（重試次數、錯誤總數）
- 錯誤明細區段（按類型分組，顯示位置、上下文、建議）
- 問題回應摘要

### 錯誤報告輸出示例

```
## Markdown 格式驗證失敗

**重試次數**: 1/5
**錯誤總數**: 3

### 錯誤明細

#### 列表格式錯誤
- **行 15, 欄 1**: 列表項目應使用 `-` 或數字加 `.` 開頭
  - 上下文: `* 項目一`

#### 程式碼區塊未閉合
- **行 22, 欄 1**: 程式碼區塊缺少結束的 ``` 標記
  - 上下文: `java public void test() {`

#### 表格格式錯誤
- **行 35, 欄 10**: 表格每行必須以 `|` 結尾
  - 上下文: `| 欄位 A | 欄位 B`
```

---

## 串流回應處理

### 串流模式特殊處理

串流模式下提供兩種策略：

**策略 A：繞過驗證**
- 保持串流即時性
- 透過 `streamingBypassValidation` 配置啟用

**策略 B：完整生成後驗證**
- 先完整生成回應
- 驗證通過後分塊發送
- 驗證失敗則重新生成（邏輯與非串流類似）

### AIServiceConfig 擴展

```java
public record AIServiceConfig(
    // ... 現有欄位

    @DefaultValue("true")
    boolean enableMarkdownValidation(),

    @DefaultValue("false")
    boolean streamingBypassValidation(),

    @DefaultValue("5")
    int maxMarkdownValidationRetries()
) {}
```

---

## 依賴配置與模組組裝

### Maven 依賴

```xml
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.22.0</version>
</dependency>

<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark-ext-gfm-tables</artifactId>
    <version>0.22.0</version>
</dependency>

<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark-ext-task-list-items</artifactId>
    <version>0.22.0</version>
</dependency>
```

### Dagger 模組配置

```java
@Module
public interface MarkdownValidationModule {

    @Provides
    @Singleton
    static MarkdownValidator provideMarkdownValidator() {
        Parser parser = Parser.builder()
            .extensions(Arrays.asList(
                TablesExtension.create(),
                TaskListItemsExtension.create()
            ))
            .build();

        HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(Arrays.asList(
                TablesExtension.create(),
                TaskListItemsExtension.create()
            ))
            .build();

        return new CommonMarkValidator(parser, renderer);
    }

    @Provides
    @Singleton
    static AIChatService provideValidatingAIChatService(
            AIServiceConfig config,
            LangChain4jAIChatService delegateService,
            MarkdownValidator validator,
            MarkdownErrorFormatter formatter) {

        if (!config.enableMarkdownValidation()) {
            return delegateService;
        }

        return new MarkdownValidatingAIChatService(
            delegateService, validator, true, formatter);
    }
}
```

---

## 測試策略

### 單元測試結構

```
src/test/java/ltdjms/discord/markdown/
├── unit/
│   ├── validation/
│   │   ├── CommonMarkValidatorTest.java
│   │   ├── MarkdownErrorFormatterTest.java
│   │   └── DiscordCompatibilityCheckerTest.java
│   └── services/
│       └── MarkdownValidatingAIChatServiceTest.java
└── integration/
    └── MarkdownValidationIntegrationTest.java
```

### 主要測試案例

**CommonMarkValidatorTest**
- 檢測未閉合的程式碼區塊
- 檢測格式錯誤的列表
- 正確的 Markdown 應通過驗證

**MarkdownValidatingAIChatServiceTest**
- 第一次成功應直接返回結果
- 格式錯誤應觸發重試並在第二次成功
- 超過重試次數應返回最後結果

---

## 實作檔案清單

### 新增檔案

```
src/main/java/ltdjms/discord/markdown/
├── validation/
│   ├── MarkdownValidator.java
│   ├── CommonMarkValidator.java
│   └── MarkdownErrorFormatter.java
└── services/
    └── MarkdownValidatingAIChatService.java

src/main/java/ltdjms/discord/shared/di/
└── MarkdownValidationModule.java

src/test/java/ltdjms/discord/markdown/
├── unit/validation/CommonMarkValidatorTest.java
├── unit/validation/MarkdownErrorFormatterTest.java
└── unit/services/MarkdownValidatingAIChatServiceTest.java
```

### 修改檔案

```
src/main/java/ltdjms/discord/aichat/domain/AIServiceConfig.java
src/main/resources/application.conf
pom.xml
```

---

## 未來擴展

### 可能的改進方向

1. **自適應重試策略**：根據錯誤類型動態調整重試提示詞
2. **格式統計收集**：收集常見錯誤類型用於改善系統提示詞
3. **格式修復模式**：嘗試自動修復簡單格式錯誤而非重新生成
4. **Discord 特定規則集**：擴展檢查規則以覆蓋更多 Discord 渲染限制
