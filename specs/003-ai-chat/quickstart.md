# Quickstart Guide: AI Chat Mentions

**Feature**: AI Chat Mentions (003-ai-chat)
**Date**: 2025-12-28
**Status**: Draft

## Overview

本指南提供 AI Chat 功能的快速入門說明，包括配置、使用方式與驗收測試。

---

## Prerequisites

- Java 17+ 已安裝
- Maven 3.8+ 已安裝
- Discord Bot Token 已取得
- AI 服務 API 金鑰已取得（OpenAI 或相容服務）

---

## Step 1: 配置環境變數

在專案根目錄的 `.env` 檔案中新增以下配置：

```bash
# Discord Bot Token（現有）
DISCORD_BOT_TOKEN=your_discord_bot_token_here

# AI 服務配置（新增）
AI_SERVICE_BASE_URL=https://api.openai.com/v1
AI_SERVICE_API_KEY=your_ai_service_api_key_here
AI_SERVICE_MODEL=gpt-3.5-turbo
AI_SERVICE_TEMPERATURE=0.7
AI_SERVICE_MAX_TOKENS=500
AI_SERVICE_TIMEOUT_SECONDS=30
```

### 配置說明

| 變數名稱 | 必要 | 預設值 | 說明 |
|---------|:----:|--------|------|
| `AI_SERVICE_BASE_URL` | ✅ | `https://api.openai.com/v1` | AI 服務 Base URL |
| `AI_SERVICE_API_KEY` | ✅ | - | AI 服務 API 金鑰 |
| `AI_SERVICE_MODEL` | ✅ | `gpt-3.5-turbo` | 模型名稱 |
| `AI_SERVICE_TEMPERATURE` | - | `0.7` | 溫度 (0.0-2.0) |
| `AI_SERVICE_MAX_TOKENS` | - | `500` | 最大 Token 數 (1-4096) |
| `AI_SERVICE_TIMEOUT_SECONDS` | - | `30` | 連線逾時秒數 (1-120，不限制推理時間) |

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

#### 其他相容服務
任何提供 OpenAI 相容 API 的服務都可以使用，例如：
- Anthropic Claude (透過相容層)
- 百度文心一言
- 阿里通義千問
- Google Gemini (透過相容層)

---

## Step 2: 建置專案

```bash
# 建置專案（跳過測試）
make build

# 或使用 Maven
mvn clean install -DskipTests
```

---

## Step 3: 執行機器人

```bash
# 使用 Docker（推薦）
make start-dev

# 或本地執行
make run
```

---

## Step 4: 驗證功能

### 基本測試

1. **在 Discord 頻道中提及機器人**：

```
@LTDJMSBot 你好
```

2. **預期行為**：
   - 機器人應在 5 秒內回應 AI 生成的訊息
   - 回應會發送到同一個頻道

### 空訊息測試

```
@LTDJMSBot
```

**預期行為**：機器人會發送預設問候語

### 長訊息測試

```
@LTDJMSBot 請詳細解釋量子物理學的基本原理，包括波粒二象性、測不準原理、量子糾纏等概念...
```

**預期行為**：如果 AI 回應超過 2000 字元，會自動分割為多則訊息

---

## Step 5: 驗收測試檢查清單

### User Story 1 - AI 回應提及機器人訊息 (P1)

- [ ] 1.1 使用者提及機器人後，機器人應在 5 秒內回傳 AI 生成的回應
- [ ] 1.2 使用者僅提及機器人（無其他內容）時，機器人應回傳預設問候語
- [ ] 1.3 使用者在不同頻道提及機器人時，機器人應在各自頻道回傳回應
- [ ] 1.4 多位使用者同時提及機器人時，每位使用者都應收到獨立的 AI 回應

### User Story 2 - 管理員配置 AI 服務參數 (P2)

- [ ] 2.1 .env 檔案包含有效的 AI 服務配置時，服務應正常啟動並使用配置的 AI 服務
- [ ] 2.2 修改 .env 中的模型名稱並重啟服務後，機器人應使用新模型生成回應
- [ ] 2.3 .env 檔案缺少必要的 API 金鑰時，服務應拒絕啟動並顯示明確的錯誤訊息
- [ ] 2.4 .env 檔案包含無效的 base URL 時，使用者提及機器人應收到錯誤提示

### User Story 3 - 錯誤處理與降級 (P3)

- [ ] 3.1 AI 服務回應 HTTP 401 時，機器人應回傳「AI 服務認證失敗，請聯絡管理員」
- [ ] 3.2 AI 服務回應 HTTP 429 時，機器人應回傳「AI 服務暫時忙碌，請稍後再試」
- [ ] 3.3 AI 服務連線逾時時，機器人應回傳「AI 服務連線逾時，請稍後再試」
- [ ] 3.4 AI 服務回應空內容時，機器人應回傳「AI 沒有產生回應」

---

## 常見問題排除

### 問題 1: 機器人沒有回應

**可能原因**：
1. 機器人在該頻道沒有「傳送訊息」權限
2. AI 服務配置錯誤
3. 網路連線問題

**解決方法**：
```bash
# 檢查機器人日誌
make logs

# 驗證 .env 配置
cat .env | grep AI_SERVICE

# 測試 AI 服務連線
curl -X POST https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AI_SERVICE_API_KEY" \
  -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"test"}]}'
```

### 問題 2: 服務啟動失敗，顯示配置錯誤

**可能原因**：
1. .env 檔案缺少必要變數
2. 溫度或最大 Token 數超出範圍

**解決方法**：
```bash
# 檢查 .env 配置
cat .env

# 確認溫度範圍 (0.0-2.0)
# 確認最大 Token 數範圍 (1-4096)
# 確認連線逾時秒數範圍 (1-120)
```

### 問題 3: AI 回應速度慢

**可能原因**：
1. 最大 Token 數過高
2. 模型較慢或負載過高
3. 網路延遲

**解決方法**：
```bash
# 減少最大 Token 數
AI_SERVICE_MAX_TOKENS=200

# 選擇更快的模型
AI_SERVICE_MODEL=gpt-3.5-turbo
```

### 問題 4: 收到「AI 服務暫時忙碌」訊息

**可能原因**：
1. AI 服務速率限制
2. 並行請求過多

**解決方法**：
- 等待一段時間後重試
- 考慮升級 AI 服務方案
- 減少並行請求數量

---

## 效能指標

根據成功標準，以下預期的效能指標：

| 指標 | 目標值 | 測試方法 |
|------|--------|---------|
| AI 回應時間 | < 5 秒 (95th percentile) | 提及機器人並測量回應時間 |
| 並行處理能力 | 100 個同時請求 | 使用多個頻道同時提及機器人 |
| AI 服務成功率 | > 95% | 監控日誌中的錯誤率 |
| 錯誤回應時間 | < 3 秒 | 模擬 AI 服務失敗 |

---

## 下一步

### 功能擴展

- [ ] 支援串流式回應 (SSE)
- [ ] 支援對話歷史保存
- [ ] 支援多個 AI 服務切換
- [ ] 支援系統提示詞配置
- [ ] 支援私人訊息 (DM) 中的 AI 聊天

### 監控與日誌

查看日誌以了解 AI 服務使用情況：

```bash
# 追蹤所有日誌
make logs

# 過濾 AI 相關日誌
make logs | grep AI
```

日誌會記錄以下資訊：
- 每次請求的時間戳
- 使用的模型名稱
- Token 使用量
- 回應時間
- 錯誤訊息（如有）

---

## 參考資料

- [OpenAI API 文件](https://platform.openai.com/docs/api-reference/chat)
- [Discord Bot 權限指南](https://discord.com/developers/docs/topics/permissions)
- [專案架構文件](../../docs/architecture/overview.md)
- [測試策略文件](../../docs/development/testing.md)
