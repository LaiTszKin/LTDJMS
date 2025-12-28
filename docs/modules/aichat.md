# AI Chat Module

## 概述

AI Chat 模組提供 Discord 機器人的 AI 聊天功能。當使用者在 Discord 頻道中提及機器人時，機器人會使用 AI 服務生成並發送回應訊息。

## 架構

### 分層設計

```
ltdjms.discord.aichat/
├── domain/           # 領域模型
│   ├── AIServiceConfig.java
│   ├── AIChatRequest.java
│   ├── AIChatResponse.java
│   └── AIMessageEvent.java
├── services/         # 服務層
│   ├── AIChatService.java (interface)
│   ├── DefaultAIChatService.java
│   └── AIClient.java
└── commands/         # JDA 事件處理
    └── AIChatMentionListener.java
```

### 組件說明

#### AIServiceConfig

AI 服務配置，包含連線資訊與參數：

- `baseUrl`: AI 服務 Base URL（預設: `https://api.openai.com/v1`）
- `apiKey`: API 金鑰（必填）
- `model`: 模型名稱（預設: `gpt-3.5-turbo`）
- `temperature`: 溫度 0.0-2.0（預設: 0.7）
- `maxTokens`: 最大 Token 數 1-4096（預設: 500）
- `timeoutSeconds`: 逾時秒數 1-120（預設: 30）

#### AIChatRequest / AIChatResponse

符合 OpenAI Chat Completions API 標準的請求與回應模型。

#### AIClient

使用 Java 17 內建 HttpClient 與 AI 服務通訊的 HTTP 客戶端。

#### DefaultAIChatService

處理 AI 請求的主要服務，包括：
- 建立請求
- 呼叫 AI 服務
- 分割長訊息（Discord 2000 字元限制）
- 發布事件

#### AIChatMentionListener

JDA 事件監聽器，監聽使用者的機器人提及並觸發 AI 回應。

## 配置

在 `.env` 檔案中配置 AI 服務：

```bash
# AI 服務配置
AI_SERVICE_BASE_URL=https://api.openai.com/v1
AI_SERVICE_API_KEY=your_api_key_here
AI_SERVICE_MODEL=gpt-3.5-turbo
AI_SERVICE_TEMPERATURE=0.7
AI_SERVICE_MAX_TOKENS=500
AI_SERVICE_TIMEOUT_SECONDS=30
```

### AI 服務供應商範例

#### OpenAI
```bash
AI_SERVICE_BASE_URL=https://api.openai.com/v1
AI_SERVICE_API_KEY=sk-...
AI_SERVICE_MODEL=gpt-3.5-turbo
```

#### Azure OpenAI
```bash
AI_SERVICE_BASE_URL=https://your-resource.openai.azure.com/openai/deployments/your-deployment
AI_SERVICE_API_KEY=your-azure-api-key
AI_SERVICE_MODEL=gpt-35-turbo
```

#### 本地模型 (Ollama)
```bash
AI_SERVICE_BASE_URL=http://localhost:11434/v1
AI_SERVICE_API_KEY=ollama
AI_SERVICE_MODEL=llama2
```

## 使用方式

### 提及機器人

在 Discord 頻道中提及機器人即可觸發 AI 回應：

```
@LTDJMSBot 你好
```

如果訊息為空（僅提及），會使用預設問候語「你好」。

### 錯誤處理

當 AI 服務發生錯誤時，會顯示友善的錯誤訊息：

| 錯誤類型 | 使用者看到的訊息 |
|---------|-----------------|
| 認證失敗 | `:x: AI 服務認證失敗，請聯絡管理員` |
| 速率限制 | `:timer: AI 服務暫時忙碌，請稍後再試` |
| 逾時 | `:hourglass: AI 服務回應逾時，請稍後再試` |
| 服務不可用 | `:warning: AI 服務暫時無法使用` |
| 空回應 | `:question: AI 沒有產生回應` |
| 格式錯誤 | `:warning: AI 回應格式錯誤` |

## 事件

### AIMessageEvent

當 AI 訊息發送時會發布 `AIMessageEvent`，包含：
- `guildId`: Discord 伺服器 ID
- `channelId`: Discord 頻道 ID
- `userId`: 使用者 ID
- `userMessage`: 使用者原始訊息
- `aiResponse`: AI 回應內容
- `timestamp`: 事件時間戳

## 日誌

日誌使用結構化格式，包含以下 MDC 欄位：
- `channel_id`: Discord 頻道 ID
- `user_id`: 使用者 ID
- `model`: AI 模型名稱

日誌等級：
- `ERROR`: AI 服務呼叫失敗、認證錯誤
- `WARN`: 速率限制、逾時、空回應
- `INFO`: AI 請求成功、回應時間

## 限制

- **無對話歷史**: 系統不保存對話歷史，每次請求都是獨立的
- **訊息長度**: 單則訊息限制 2000 字元（Discord 限制），超過會自動分割
- **逾時**: AI 服務逾時設定為 30 秒（可配置）
- **並行**: 支援多個並行請求

## 測試

### 單元測試

```bash
# 執行 AI Chat 模組的所有單元測試
mvn test -Dtest='ltdjms.discord.aichat.unit.*'

# 執行特定測試類別
mvn test -Dtest=AIClientTest
mvn test -Dtest=AIChatServiceTest
mvn test -Dtest=MessageSplitterTest
```

### 整合測試

```bash
# 執行 AI Chat 整合測試
mvn test -Dtest='ltdjms.discord.aichat.integration.*'
```

## 相關文件

- [AI Chat 規格](../../specs/003-ai-chat/spec.md)
- [AI Chat 實作計畫](../../specs/003-ai-chat/plan.md)
- [AI Chat 快速入門](../../specs/003-ai-chat/quickstart.md)
- [系統架構](../architecture/overview.md)
