# Getting Started

## 1. Purpose

- Project purpose: 在 Discord guild 內提供經濟系統、商品商店、護航派單與 AI 輔助能力
- Primary users: Discord 伺服器管理員、維運人員、Java 後端開發者
- Supported environments: 本機開發、Docker Compose 單機部署、可對外提供 ECPay callback 的主機環境

## 2. Prerequisites

- Runtime/tooling:
  - Java 17
  - Maven 3.8+
  - Docker 與 Docker Compose（建議）
- Accounts/services needed:
  - Discord Bot application 與 `DISCORD_BOT_TOKEN`
  - PostgreSQL
  - Redis
  - AI provider API key（目前啟動時即需要）
  - ECPay merchant credentials（僅啟用法幣付款時需要）
- Local dependencies:
  - `prompts/` 目錄（若要提供外部提示詞）
  - 可寫入的日誌目錄（預設 `logs/`；Docker 為 `/app/logs`）

## 3. Installation

```bash
git clone <your-repo-url>
cd LTDJMS
cp .env.example .env
mkdir -p prompts
make build
```

補充說明：
- `make build` 會執行 `mvn clean package -DskipTests`
- 若要先同步缺漏的 `.env` 項目，可使用 `make setup-env`
- 若你偏好先驗證測試，再繼續開發，可執行 `make test` 或 `make verify`

## 4. Local Run

### Docker Compose（推薦）

```bash
make start-dev
make logs
```

這條路徑會啟動：
- `postgres`（PostgreSQL 16）
- `redis`（Redis 7）
- `bot`（Java 17 runtime 容器）

### 本機 JVM 直跑

```bash
make db-up
make build
java -jar target/ltdjms-*.jar
```

注意事項：
- `Makefile` 目前沒有實作 `run` target，請直接使用 `java -jar`
- 若使用本機直跑且需要 Redis，請自行確保 `REDIS_URI` 指向可連線的 Redis
- 若未設定 `ECPAY_RETURN_URL`，綠界 callback server 會直接跳過啟動

## 5. Deployment

- Deployment targets: 以 `docker compose up -d` 為主的單機容器部署
- Deployment command or pipeline:

```bash
make build
make start
```

- Required pre-deploy checks:
  - `.env` / 環境變數已提供必要 secrets
  - PostgreSQL schema 可接受啟動時的 Flyway migration
  - `AI_SERVICE_API_KEY` 已設定；`AI_SERVICE_BASE_URL` 若未自訂可使用內建預設值
  - 若啟用綠界付款，`ECPAY_*` 參數與公開 callback 路徑已完成配置
- Rollback notes:
  - `Unknown`：repo 沒有提供專用 rollback script 或 CI release workflow
  - 依程式碼現況，回滾時需自行確認舊版映像與最新資料庫 migration 的相容性

## 6. Verification

- Smoke checks:
  - `make logs` 應看到 bot 啟動、JDA ready 與 slash command sync 記錄
  - Discord 中 Bot 應顯示上線，且 guild 內可看見 `/user-panel`、`/shop` 等指令
  - 管理員帳號可使用 `/admin-panel` 與 `/dispatch-panel`
  - 若設定 `ECPAY_RETURN_URL`，日誌應出現 `ECPay callback server started`
- Expected signals:
  - 啟動成功：`LTDJ management system started successfully!`
  - 缺少 Bot Token：啟動階段丟出 `Discord bot token not configured`
  - 缺少 AI API key（啟用 AI 時）：啟動階段丟出 `AI service API key not configured`
- Evidence:
  - `Makefile`
  - `docker-compose.yml`
  - `Dockerfile`
  - `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
  - `src/main/java/ltdjms/discord/shop/services/EcpayCallbackHttpServer.java`
