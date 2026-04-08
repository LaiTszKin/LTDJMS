# 系統架構

## 先用一句話理解

LTDJMS 以 Discord 互動為入口，透過 Dagger 組裝 guild 級經濟、商品、派單與 AI 模組，將狀態寫入 PostgreSQL、用 Redis 做快取，並在需要時串接 ECPay 與外部履約服務。

## 系統入口

- Slash commands：`/currency-config`、`/dice-game-1`、`/dice-game-2`、`/user-panel`、`/admin-panel`、`/shop`、`/dispatch-panel`
- 元件互動：按鈕、select menu、modal
- 訊息事件：允許頻道中的 Bot mention
- HTTP 入口：`EcpayCallbackHttpServer` 提供首頁與付款回推 endpoint

主程式入口：

- `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
- `src/main/java/ltdjms/discord/currency/bot/SlashCommandListener.java`

## 模組地圖

| 路徑 | 主要責任 |
| --- | --- |
| `src/main/java/ltdjms/discord/currency` | guild 貨幣設定、餘額、交易與 slash command |
| `src/main/java/ltdjms/discord/gametoken` | 遊戲代幣帳戶、骰子遊戲、代幣交易 |
| `src/main/java/ltdjms/discord/panel` | `/user-panel`、`/admin-panel`、session 與互動畫面 |
| `src/main/java/ltdjms/discord/product` | 商品定義、驗證、建立與更新 |
| `src/main/java/ltdjms/discord/redemption` | 兌換碼生成、驗證與兌換紀錄 |
| `src/main/java/ltdjms/discord/shop` | 商店顯示、貨幣購買、法幣訂單、ECPay callback、履約與通知 |
| `src/main/java/ltdjms/discord/dispatch` | 護航派單、完單、售後人員與定價管理 |
| `src/main/java/ltdjms/discord/aichat` | AI 頻道限制、提及式聊天、提示詞、串流處理 |
| `src/main/java/ltdjms/discord/aiagent` | Agent 頻道配置、對話記憶、工具執行與審計 |
| `src/main/java/ltdjms/discord/markdown` | AI 回應 Markdown 驗證、切段與自動修正 |
| `src/main/java/ltdjms/discord/discord` | Discord 抽象層、adapter、embed builder、session abstraction |
| `src/main/java/ltdjms/discord/shared` | `EnvironmentConfig`、資料庫、快取、事件、DI、共用型別 |

## 核心分層

多數業務模組都遵守相同拆分：

- `domain/`：核心資料模型與 repository 介面
- `persistence/`：JDBC / jOOQ 儲存實作
- `services/`：業務流程與跨模組協作
- `commands/`：Discord 事件協調、輸入驗證與回應組裝

這裡的慣例是：

- handler 盡量只做 Discord 協調
- 服務層用 `Result<T, DomainError>` 回傳失敗
- 副作用與快取失效優先透過事件管線解耦

## 主要資料流

### 1. Slash command / 面板互動

1. JDA 收到 slash command 或 component interaction
2. `SlashCommandListener` / button handler 分派到對應 handler
3. handler 呼叫 service 與 repository
4. service 視需要發佈 `DomainEvent`
5. listener 更新面板、失效快取或同步 AI Agent 相關狀態

### 2. 商品購買與付款後履約

1. `/shop` 觸發貨幣購買或法幣下單
2. 貨幣購買由 `CurrencyPurchaseService` 扣款、記錄交易、發放獎勵
3. 法幣購買由 `FiatOrderService` 建立待付款訂單並向 ECPay 取號
4. `EcpayCallbackHttpServer` 接收回推後交給 `FiatPaymentCallbackService`
5. callback 會做驗證、冪等更新，最後觸發履約或管理員通知

### 3. AI Chat / AI Agent

1. `AIChatMentionListener` 檢查 guild、頻道、類別是否允許
2. 一般頻道走 AI Chat；啟用 Agent 的頻道走 Agent 工具鏈
3. AI 回應可經過 Markdown 驗證與切段
4. Agent 相關設定、記憶與工具執行紀錄由 `aiagent/` 管理

## 跨模組基礎設施

### Dagger DI

- `AppComponent` 是整體依賴組裝入口
- 各 `*Module` 提供 repository、service、listener 與外部整合
- 啟動時先建立 component，再初始化資料庫與 JDA

### 事件系統

- `DomainEventPublisher` 由 `EventModule` 提供
- Dagger `@IntoSet` 收集 `Consumer<DomainEvent>`
- 分發是同步進行；單一 listener 失敗會記錄日誌，但不會中斷其他 listener

### 儲存與快取

- PostgreSQL：交易、商品、派單、AI / panel 設定、法幣訂單
- Redis：高頻設定與快取
- Filesystem：`.env`、`prompts/`、`logs/`

## 修改時最容易踩雷的地方

- ECPay callback 與履約流程有明確的冪等 claim / mark 機制，不能隨意改順序
- `DomainEventPublisher` 是同步分發，listener 變慢會直接拖慢主流程
- 面板互動高度依賴既有 custom ID / modal ID / session 管理
- AI 頻道白名單與 AI Agent 啟用狀態是兩套設定，不要混為一談
- 付款、派單、售後這類狀態流轉修改時，要一起檢查 repository、migration 與測試
