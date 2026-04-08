# 快速開始與部署

## 這份文件適合誰

- 第一次接手 LTDJMS 的開發者
- 要在本機或單機環境啟動 bot 的維運人員
- 需要確認最小啟動條件與 smoke check 的人

## 啟動前準備

### 必要工具

- Java 17
- Maven 3.8+
- Docker 與 Docker Compose（建議）

### 最低可用條件

- `DISCORD_BOT_TOKEN`
- 可連線的 PostgreSQL
- 可連線的 Redis
- `AI_SERVICE_API_KEY`

### 視功能需要再補的設定

- `prompts/` 目錄：若要載入外部提示詞
- `ECPAY_*`：若要啟用綠界超商代碼付款
- `PRODUCT_FULFILLMENT_SIGNING_SECRET`：若要呼叫外部履約 webhook

## 最短可用路徑：Docker Compose

```bash
git clone <your-repo-url>
cd ltdjms
cp .env.example .env
mkdir -p prompts
make build
make start-dev
make logs
```

這條路徑會啟動：

- `postgres`：PostgreSQL 16
- `redis`：Redis 7
- `bot`：Java 17 runtime 容器

## 本機 JVM 直跑

```bash
cp .env.example .env
mkdir -p prompts
make db-up
make build
java -jar target/ltdjms-*.jar
```

注意：

- `Makefile` 目前沒有 `make run` target。
- 若不是用 Docker Compose 啟動整套環境，請自己確認 `REDIS_URI` 與資料庫連線可用。
- 若沒有設定 `ECPAY_RETURN_URL`，callback server 會跳過啟動，這是正常行為。

## 部署建議

### 單機容器部署

```bash
make build
make start
```

### 部署前檢查

- `.env` / 環境變數已填入必要 secrets
- PostgreSQL schema 能接受啟動時的 Flyway migration
- `AI_SERVICE_API_KEY` 已設定
- 若啟用綠界付款，`ECPAY_RETURN_URL` 與 `ECPAY_CALLBACK_*` 已正確配置
- 若要做外部履約，商品的 `backendApiUrl` 與 `PRODUCT_FULFILLMENT_SIGNING_SECRET` 已準備好

## 啟動成功時你會看到什麼

### 日誌訊號

- `LTDJ management system started successfully!`
- `make logs` 中出現 JDA ready 與 slash command sync 記錄
- 若有設定 `ECPAY_RETURN_URL`，會看到 callback server 啟動訊息

### Discord 端檢查

- Bot 顯示在線
- guild 中可看到 `/user-panel`、`/shop`、`/admin-panel`、`/dispatch-panel`
- 管理員可正常打開 `/admin-panel` 與 `/dispatch-panel`

## 常見卡點

| 症狀 | 常見原因 |
| --- | --- |
| 啟動即失敗並出現 `Discord bot token not configured` | 沒有提供 `DISCORD_BOT_TOKEN` |
| 啟動即失敗並出現 `AI service API key not configured` | 沒有提供 `AI_SERVICE_API_KEY` |
| 綠界回推服務沒有啟動 | 沒有設定 `ECPAY_RETURN_URL` |
| Redis 相關初始化失敗 | `REDIS_URI` 指向的 Redis 不可連線 |
| Slash commands 沒同步 | Bot 沒加入 guild、權限不足，或 JDA 啟動異常 |
