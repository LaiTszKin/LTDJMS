# Architecture

## 1. High-Level Structure

- Entry points:
  - Slash commands：`/currency-config`, `/dice-game-1`, `/dice-game-2`, `/user-panel`, `/admin-panel`, `/shop`, `/dispatch-panel`
  - Message mentions：在允許的 guild 頻道提及 Bot 觸發 AI Chat / AI Agent
  - Component interactions：按鈕、select menu、modal 驅動面板與商店流程
  - Embedded HTTP server：接收 ECPay callback
- Major modules:
  - `currency/`：guild 貨幣設定、餘額與交易
  - `gametoken/`：遊戲代幣與骰子遊戲
  - `panel/`：`/user-panel`、`/admin-panel` 與相關互動狀態
  - `product/` + `redemption/` + `shop/`：商品、兌換碼、貨幣購買、法幣付款與履約
  - `dispatch/`：護航派單、完單確認、售後流程
  - `aichat/` + `aiagent/`：提及式 AI 對話、頻道限制、AI Agent 工具與記憶
  - `shared/`：設定、資料庫、快取、Result、事件管道、DI
- Data flow summary:
  - Discord 事件 -> JDA listeners / handlers -> services -> repositories / external APIs -> domain events -> cache / panel / agent listeners
  - ECPay callback -> `EcpayCallbackHttpServer` -> `FiatPaymentCallbackService` -> 訂單狀態更新 -> 履約 / 管理員通知
- Integration boundaries:
  - PostgreSQL：主要狀態存放與 Flyway migration
  - Redis：快取與部分 AI Agent 狀態加速
  - Discord API：JDA gateway、slash commands、DM、components
  - AI provider：OpenAI-compatible chat completions
  - ECPay：超商代碼付款與付款回推
  - External fulfillment API：商品後端履約 webhook
  - Filesystem：`.env`、`prompts/`、`logs/`

## 2. Directory Guide

| Path | Responsibility |
| --- | --- |
| `src/main/java/ltdjms/discord/currency` | 貨幣設定、餘額調整、slash command 入口 |
| `src/main/java/ltdjms/discord/gametoken` | 遊戲代幣帳戶、骰子遊戲與交易記錄 |
| `src/main/java/ltdjms/discord/panel` | 使用者 / 管理面板互動、ephemeral session、嵌入式設定流程 |
| `src/main/java/ltdjms/discord/product` | 商品模型、驗證、建立與更新 |
| `src/main/java/ltdjms/discord/redemption` | 兌換碼產生、驗證與兌換交易 |
| `src/main/java/ltdjms/discord/shop` | 商店瀏覽、貨幣購買、綠界付款、履約與通知 |
| `src/main/java/ltdjms/discord/dispatch` | 派單建立、確認、完單、售後與歷史流程 |
| `src/main/java/ltdjms/discord/aichat` | 提及式 AI Chat、訊息切片、提示詞載入、Markdown 驗證 |
| `src/main/java/ltdjms/discord/aiagent` | Agent 頻道配置、工具呼叫、記憶與工具執行事件 |
| `src/main/java/ltdjms/discord/shared` | `EnvironmentConfig`、`DatabaseConfig`、`Result`、快取、事件、Dagger 模組 |
| `src/main/resources/db/migration` | Flyway schema migrations |
| `src/test/java/ltdjms/discord/*` | 單元、整合、合約、效能與 property-based 測試 |
| `docs/api`, `docs/modules`, `docs/operations`, `docs/development` | 補充性深度文件 |

## 3. Key Flows

### Slash Commands And Panel Updates
- Trigger: 使用者或管理員執行 slash command，或點擊面板按鈕 / 選單
- Main steps:
  1. `SlashCommandListener` 根據指令名稱分派給對應 handler
  2. handler 呼叫 service 與 repository 完成業務操作
  3. service 在需要時發佈 `DomainEvent`
  4. 事件監聽器更新面板顯示、清除快取或同步 AI Agent 狀態
- Dependencies: JDA, PostgreSQL, Redis, Dagger multibinding event pipeline
- Failure boundaries: 權限不足、guild 外呼叫、資料庫寫入錯誤、事件監聽器局部失敗

### Shop Purchase And Fiat Fulfillment
- Trigger: 使用者在 `/shop` 中購買商品，或 ECPay 對 callback endpoint 發送付款結果
- Main steps:
  1. 貨幣購買：扣款 -> 記錄交易 -> 發放商品獎勵 -> 視需要呼叫後端履約
  2. 法幣下單：向 ECPay 取號 -> 建立 `PENDING` 法幣訂單 -> 私訊買家超商代碼
  3. 付款回推：驗證與解密 payload -> `markPaidIfPending` -> 觸發履約與管理員通知一次
- Dependencies: PostgreSQL, ECPay, external fulfillment API, Discord DM
- Failure boundaries: 支付設定缺漏、ECPay callback 驗證失敗、履約 webhook 失敗、DM 送達失敗

### AI Chat And Agent Channel Governance
- Trigger: 使用者在允許的頻道提及 Bot
- Main steps:
  1. `AIChatMentionListener` 檢查 guild / 頻道限制與 agent enable 狀態
  2. 載入外部提示詞與 AI provider 設定
  3. 執行串流或緩衝回應；Agent 模式可呼叫 Discord 工具與記錄事件
  4. 選擇性顯示 reasoning，並在需要時做 Markdown 驗證與重整
- Dependencies: AI provider, Redis cache, PostgreSQL-backed agent config, filesystem prompts
- Failure boundaries: API key 缺失、頻道不在允許清單、模型供應商逾時 / 授權失敗、Discord 訊息長度限制

### Unified Domain Event Pipeline
- Trigger: 各模組 service 發佈 `DomainEvent`
- Main steps:
  1. Dagger `@IntoSet` 收集 `Consumer<DomainEvent>`
  2. `EventModule` 建立單一 `DomainEventPublisher`
  3. `publish(...)` 同步逐一分發，單一 listener 例外僅記錄日誌，不中斷其他 listener
- Dependencies: `EventModule`, `CacheModule`, panel / agent listener providers
- Failure boundaries: listener 漏綁定會造成 side effect 缺失；例外會被記錄但不往外拋

## 4. Operational Notes

- State/storage model:
  - PostgreSQL：交易、商品、派單、AI / panel 設定、法幣訂單
  - Redis：快取與部分高頻配置查詢
  - Filesystem：`prompts/` 與 `logs/`
- Concurrency or ordering concerns:
  - ECPay callback 透過 `markPaidIfPending`、`claimFulfillmentProcessing`、`claimAdminNotificationProcessing` 控制冪等
  - `DomainEventPublisher` 為同步分發；listener 執行時間會直接影響原請求完成時間
  - Slash commands 採 guild-specific sync；Bot 加入新 guild 時會自動補同步
- Observability entrypoints:
  - `src/main/resources/logback.xml` 輸出 console + `logs/app.log` / `logs/warn.log` / `logs/error.log`
  - `make logs` 追蹤容器日誌
  - 測試入口：`make test`, `make test-integration`, `make verify`

## Evidence

- `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
- `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java`
- `src/main/java/ltdjms/discord/shared/di/AppComponent.java`
- `src/main/java/ltdjms/discord/shared/di/EventModule.java`
- `src/main/java/ltdjms/discord/shared/events/DomainEventPublisher.java`
- `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
- `src/main/java/ltdjms/discord/shop/services/FiatPaymentCallbackService.java`
- `src/main/java/ltdjms/discord/aichat/commands/AIChatMentionListener.java`
