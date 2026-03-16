# LTDJMS

LTDJMS 是一個以 Java 17、Maven 與 JDA 建置的 Discord 管理機器人，提供伺服器貨幣、遊戲代幣、商品商店、兌換碼、法幣付款、護航派單，以及 AI 聊天 / AI Agent 頻道治理能力。

## Highlights

- Guild 級虛擬貨幣、遊戲代幣與管理面板
- 商品、兌換碼、貨幣購買與綠界超商代碼付款
- 護航派單、完單確認與售後流程
- 提及式 AI Chat、AI 頻道限制與 AI Agent 頻道配置

## Quick Start

### Install

```bash
git clone <your-repo-url>
cd LTDJMS
cp .env.example .env
mkdir -p prompts
make build
```

### Configure

- `DISCORD_BOT_TOKEN`：必要，Discord Bot 憑證
- 資料庫：設定 `DB_URL`，或改用 `DATABASE_HOST` / `DATABASE_PORT` / `DATABASE_NAME` 搭配 `DB_USERNAME` / `DB_PASSWORD`
- AI 功能：目前應用在啟動時就會驗證 `AI_SERVICE_API_KEY`，若缺少此值會直接啟動失敗
- 法幣付款：啟用綠界流程時需設定 `ECPAY_*` 變數

### Run or Deploy

```bash
make start-dev
# 或本機直接執行
java -jar target/ltdjms-*.jar
```

## Documentation

- 文件索引：`docs/README.md`
- 安裝與部署：`docs/getting-started.md`
- 設定與外部服務：`docs/configuration.md`
- 系統架構：`docs/architecture.md`
- 功能導覽：`docs/features.md`
- 開發者指南：`docs/developer-guide.md`

## Main Features

### 經濟系統與互動面板
透過 `/user-panel`、`/admin-panel`、`/currency-config` 與骰子遊戲指令，管理 guild 專屬貨幣、遊戲代幣、餘額調整與遊戲設定。

### 商品、兌換與履約
支援 `/shop` 商品瀏覽、貨幣購買、兌換碼發放與兌換，並可在付款完成後觸發後端履約或管理員通知。

### 派單與 AI 能力
支援 `/dispatch-panel` 護航派單流程，以及在受限頻道中透過提及機器人啟用 AI Chat / AI Agent。

## Notes

- Docker Compose 是目前最完整的本機與單機部署路徑，會同時啟動 bot、PostgreSQL 與 Redis。
- `Makefile` 目前沒有可執行的 `run` target；本機直跑請使用 `java -jar target/ltdjms-*.jar`。
- 舊版細節文件仍保留在 `docs/api/`、`docs/modules/`、`docs/operations/` 等目錄作為補充參考。
