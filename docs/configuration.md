# Configuration

## 1. Environment Variables And Config Files

`EnvironmentConfig` 的載入優先序為：系統環境變數 -> 專案根目錄 `.env` -> `application.conf` / `application.properties` -> 內建預設值。

### 主要設定檔

| Key / File | Required | Purpose | Example / Default | Evidence |
| --- | --- | --- | --- | --- |
| `.env.example` | No | 本機設定範本 | 專案根目錄範本檔 | `.env.example` |
| `.env` | No | 本機覆蓋設定 | 由開發者建立 | `src/main/java/ltdjms/discord/shared/DotEnvLoader.java` |
| `src/main/resources/application.properties` | No | 保留預設值與說明文字 | `db.url=jdbc:postgresql://localhost:5432/currency_bot` | `src/main/resources/application.properties` |
| `src/main/resources/application.conf` | No | 舊式 Typesafe Config 鍵名與資料庫預設 | `database.host=localhost` | `src/main/resources/application.conf` |
| `src/main/resources/logback.xml` | No | 日誌路徑、等級與輪替設定 | `LOG_DIR=logs` | `src/main/resources/logback.xml` |
| `docker-compose.yml` | No | 容器部署時的環境變數映射 | `PROMPTS_DIR_PATH=/app/prompts` | `docker-compose.yml` |
| `prompts/` | No | 外部提示詞 Markdown 檔案目錄 | `./prompts` | `src/main/java/ltdjms/discord/aichat/services/DefaultPromptLoader.java` |

### 環境變數

| Key / File | Required | Purpose | Example / Default | Evidence |
| --- | --- | --- | --- | --- |
| `DISCORD_BOT_TOKEN` | Yes | Discord Bot 登入 Token | `your_discord_bot_token_here` | `.env.example`, `EnvironmentConfig.java` |
| `DB_URL` | No | 直接指定 JDBC URL；若有值會覆蓋 `DATABASE_*` 組合 | `jdbc:postgresql://localhost:5432/currency_bot` | `.env.example`, `EnvironmentConfig.java` |
| `DATABASE_HOST` | No | 組合式資料庫 host | `localhost` | `.env.example`, `EnvironmentConfig.java` |
| `DATABASE_PORT` | No | 組合式資料庫 port | `5432` | `.env.example`, `EnvironmentConfig.java` |
| `DATABASE_NAME` | No | 組合式資料庫名稱 | `currency_bot` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `DB_USERNAME` | No | 資料庫帳號 | `postgres` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `DB_PASSWORD` | No | 資料庫密碼 | `postgres` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `DATABASE_USER` | No | 舊式資料庫帳號別名 | `postgres` | `application.conf`, `EnvironmentConfig.java` |
| `DATABASE_PASSWORD` | No | 舊式資料庫密碼別名 | `postgres` | `application.conf`, `EnvironmentConfig.java` |
| `DB_POOL_MAX_SIZE` | No | HikariCP 最大連線數 | `10` | `.env.example`, `application.properties`, `EnvironmentConfig.java` |
| `DB_POOL_MIN_IDLE` | No | HikariCP 最小閒置數 | `2` | `.env.example`, `application.properties`, `EnvironmentConfig.java` |
| `DB_POOL_CONNECTION_TIMEOUT` | No | 取得連線逾時毫秒數 | `30000` | `.env.example`, `application.properties`, `EnvironmentConfig.java` |
| `DB_POOL_IDLE_TIMEOUT` | No | 閒置連線逾時毫秒數 | `600000` | `.env.example`, `application.properties`, `EnvironmentConfig.java` |
| `DB_POOL_MAX_LIFETIME` | No | 連線最長存活毫秒數 | `1800000` | `.env.example`, `application.properties`, `EnvironmentConfig.java` |
| `REDIS_URI` | No | Redis 連線 URI | `redis://localhost:6379` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `ECPAY_MERCHANT_ID` | No | 綠界 Merchant ID | 空字串 | `.env.example`, `EnvironmentConfig.java` |
| `ECPAY_HASH_KEY` | No | 綠界加密 HashKey | 空字串 | `.env.example`, `EnvironmentConfig.java` |
| `ECPAY_HASH_IV` | No | 綠界加密 HashIV | 空字串 | `.env.example`, `EnvironmentConfig.java` |
| `ECPAY_RETURN_URL` | No | 綠界付款回推 URL；未設時 callback server 不啟動 | 空字串 | `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java` |
| `ECPAY_STAGE_MODE` | No | 是否使用綠界測試端點 | `true` | `.env.example`, `EnvironmentConfig.java` |
| `ECPAY_CVS_EXPIRE_MINUTES` | No | 超商代碼有效分鐘數 | `10080` | `.env.example`, `EnvironmentConfig.java` |
| `ECPAY_CALLBACK_BIND_HOST` | No | 內嵌 callback server 綁定 host | `127.0.0.1` | `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java` |
| `ECPAY_CALLBACK_BIND_PORT` | No | 內嵌 callback server 綁定 port | `8085` | `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java` |
| `ECPAY_CALLBACK_PATH` | No | 內嵌 callback server 路徑 | `/ecpay/callback` | `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java` |
| `ECPAY_CALLBACK_SHARED_SECRET` | No | callback query token；公開綁定時必填 | 空字串 | `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java` |
| `PRODUCT_FULFILLMENT_SIGNING_SECRET` | No | 對外商品履約 webhook HMAC 密鑰 | 空字串 | `.env.example`, `EnvironmentConfig.java`, `ProductFulfillmentApiService.java` |
| `AI_SERVICE_BASE_URL` | No | OpenAI 相容 API base URL | `https://api.openai.com/v1` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `AI_SERVICE_API_KEY` | Yes | AI 服務 API key；目前啟動時即會驗證 | `your_ai_service_api_key_here` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `AI_SERVICE_MODEL` | No | AI 模型名稱 | `gpt-4o-mini` in `.env.example`; runtime built-in default `gpt-3.5-turbo` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `AI_SERVICE_TEMPERATURE` | No | AI 溫度參數 | `0.7` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `AI_SERVICE_TIMEOUT_SECONDS` | No | AI 連線逾時秒數 | `30` | `.env.example`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `AI_SHOW_REASONING` | No | 是否顯示 AI 推理片段 | `false` | `.env.example`, `application.properties`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `PROMPTS_DIR_PATH` | No | 外部提示詞資料夾路徑 | `./prompts` | `.env.example`, `application.properties`, `EnvironmentConfig.java`, `docker-compose.yml` |
| `PROMPT_MAX_SIZE_BYTES` | No | 單一提示詞檔案大小上限 | `1048576` | `.env.example`, `EnvironmentConfig.java` |
| `AI_MARKDOWN_VALIDATION_ENABLED` | No | 是否啟用 Markdown 驗證 | `true` | `.env.example`, `EnvironmentConfig.java` |
| `AI_MARKDOWN_VALIDATION_STREAMING_BYPASS` | No | 串流時是否跳過 Markdown 驗證 | `false` | `.env.example`, `EnvironmentConfig.java` |
| `LOG_LEVEL` | No | root logger 等級 | `WARN` | `.env.example`, `logback.xml`, `docker-compose.yml` |
| `APP_LOG_LEVEL` | No | `ltdjms.discord.*` logger 等級 | `INFO` | `.env.example`, `logback.xml`, `docker-compose.yml` |
| `LOG_DIR` | No | 日誌輸出路徑 | `logs` | `.env.example`, `logback.xml`, `docker-compose.yml` |
| `LOG_MAX_FILE_SIZE` | No | 單個輪替檔最大大小 | `20MB` | `.env.example`, `logback.xml`, `docker-compose.yml` |
| `LOG_MAX_HISTORY_DAYS` | No | 保留天數 | `30` | `.env.example`, `logback.xml`, `docker-compose.yml` |
| `LOG_TOTAL_SIZE_CAP` | No | 所有歷史檔案總容量上限 | `3GB` | `.env.example`, `logback.xml`, `docker-compose.yml` |

## 2. External Services And Credential Setup

### Discord

- Purpose: Bot 登入 Discord、同步 slash commands、接收互動與訊息事件
- Required keys/config: `DISCORD_BOT_TOKEN`
- Official setup entry: Discord Developer Portal (`https://discord.com/developers/applications`)
- Acquisition steps:
  1. 建立或開啟 Discord application
  2. 在 `Bot` 分頁建立 Bot，重新產生 Token
  3. 把 Token 放入 `.env` 或部署環境變數
  4. 使用 `bot` + `applications.commands` scope 邀請 Bot 進入目標 guild
- Development notes: `MESSAGE_CONTENT` intent 會在 JDA 啟動時啟用；slash commands 於 bot ready 後逐 guild 同步
- Evidence: `.env.example`, `DiscordCurrencyBot.java`, `SlashCommandListener.java`

### PostgreSQL

- Purpose: 儲存 guild 設定、帳戶、商品、兌換碼、派單與法幣訂單資料
- Required keys/config: `DB_URL` 或 `DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME`，搭配 `DB_USERNAME` / `DB_PASSWORD`
- Official setup entry: `Unknown`（repo 未包含外部託管服務申請流程）
- Acquisition steps:
  1. 建立 PostgreSQL 資料庫（Docker Compose 預設用 `currency_bot`）
  2. 建立使用者與密碼
  3. 把連線資訊放入 `.env` 或部署環境變數
  4. 啟動 bot，讓 Flyway 自動套用 migration
- Development notes: `docker-compose.yml` 已提供本機 PostgreSQL；啟動時會先跑 migration
- Evidence: `docker-compose.yml`, `EnvironmentConfig.java`, `DiscordCurrencyBot.java`, `src/main/resources/db/migration/`

### Redis

- Purpose: 快取 guild 設定、AI Agent 部分查詢結果與其他高頻讀取資料
- Required keys/config: `REDIS_URI`
- Official setup entry: `Unknown`（repo 未包含託管 Redis 開通流程）
- Acquisition steps:
  1. 啟動 Redis（Docker Compose 預設提供 `redis` service）
  2. 設定 `REDIS_URI`
  3. 啟動 bot 並確認 Redis 可連線
  4. 觀察日誌中的 Redis 初始化訊息
- Development notes: 目前 `CacheModule` 直接建立 `RedisCacheService`；沒有在 DI 層自動切換到 `NoOpCacheService`
- Evidence: `docker-compose.yml`, `CacheModule.java`, `RedisCacheService.java`, `NoOpCacheService.java`

### AI Provider (OpenAI-compatible)

- Purpose: 提供提及式 AI Chat 與 AI Agent 模型推理
- Required keys/config: `AI_SERVICE_API_KEY`; `AI_SERVICE_BASE_URL` 與 `AI_SERVICE_MODEL` 有內建預設
- Official setup entry: 依供應商而定；repo 僅明示 OpenAI-compatible base URL 模式
- Acquisition steps:
  1. 選擇 OpenAI-compatible 供應商
  2. 取得 API key 與 base URL
  3. 把設定放入 `.env` 或部署環境變數
  4. 在允許的 guild 頻道提及 Bot 驗證回應
- Development notes: `AIChatModule` 會在組裝時驗證 AI 設定，因此 API key 缺失會直接導致應用啟動失敗；可透過 `PROMPTS_DIR_PATH` 掛載外部提示詞
- Evidence: `.env.example`, `EnvironmentConfig.java`, `AIChatMentionListener.java`

### ECPay

- Purpose: 為法幣限定商品產生超商代碼，並於付款回推後觸發履約與管理員通知
- Required keys/config: `ECPAY_MERCHANT_ID`, `ECPAY_HASH_KEY`, `ECPAY_HASH_IV`, `ECPAY_RETURN_URL`, `ECPAY_STAGE_MODE`, callback 相關 `ECPAY_CALLBACK_*`
- Official setup entry: 既有 spec 引用了綠界開發者文件與測試參數頁面
- Acquisition steps:
  1. 在綠界商店後台取得 Merchant ID、HashKey、HashIV
  2. 配置可讓綠界回推的 `ECPAY_RETURN_URL`
  3. 視部署情境設定 callback bind host / port / path
  4. 若公開暴露 callback server，另行設定 `ECPAY_CALLBACK_SHARED_SECRET`
- Development notes: 若未設定 `ECPAY_RETURN_URL`，callback server 不啟動；若綁定公網位址但未設定 shared secret，啟動會直接失敗
- Evidence: `.env.example`, `EnvironmentConfig.java`, `EcpayCallbackHttpServer.java`, `FiatPaymentCallbackService.java`

### Product Fulfillment Backend

- Purpose: 商品購買或付款完成後，向外部後端發送履約 webhook
- Required keys/config: 商品上的 `backendApiUrl`，以及 `PRODUCT_FULFILLMENT_SIGNING_SECRET`
- Official setup entry: `Unknown`（由專案整合方自行提供）
- Acquisition steps:
  1. 準備支援 HTTPS 的履約端點
  2. 在商品設定中填入 `backendApiUrl`
  3. 配置 `PRODUCT_FULFILLMENT_SIGNING_SECRET`
  4. 透過貨幣購買或法幣付款完成流程驗證 webhook 是否送達
- Development notes: `backendApiUrl` 必須是 `https://`；自動建立護航訂單時還必須同時設定 `escortOptionCode`
- Evidence: `Product.java`, `ProductFulfillmentApiService.java`, `.env.example`

## 3. Safety Notes

- Secret handling: Bot Token、AI key、ECPay 憑證、履約簽章密鑰都應放在 `.env`、部署環境變數或外部 secret manager，不應提交到版本控制
- Local overrides: `DB_URL` 會優先覆蓋 `DATABASE_*` 組合；`DB_USERNAME` / `DB_PASSWORD` 也會優先於 `DATABASE_USER` / `DATABASE_PASSWORD`
- Common misconfiguration symptoms:
  - `DISCORD_BOT_TOKEN` 缺失：啟動直接失敗
  - `AI_SERVICE_API_KEY` 缺失：應用在啟動組裝 AI 模組時就會失敗
  - 公網 callback 未加 `ECPAY_CALLBACK_SHARED_SECRET`：綠界 callback server 啟動失敗
  - Redis 不可連線：目前 DI 會直接建立 `RedisCacheService`，可能在啟動期就失敗
  - `AI_SERVICE_MAX_TOKENS`：現行 `EnvironmentConfig` 不支援此變數，舊文件若提到它，應以程式碼現況為準
