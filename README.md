# LTDJMS

LTDJMS 是一個以 Java 開發的 Discord 機器人，用於協助管理業務相關的 Discord 群組，並提供一些自製小遊戲來提升群內互動。

## 專案簡介

- 使用 Maven 作為建置工具
- 使用 Java 17 + JDA 5.x (Java Discord API)
- 使用 PostgreSQL 儲存資料
- 建議使用 VS Code 或其他支援 Java 的 IDE 進行開發
- 需要在環境變數或設定檔中提供 Discord Bot Token

## 功能

### Discord 伺服器貨幣系統

允許 Discord 伺服器管理員建立和管理虛擬貨幣系統：

- **查看餘額** (`/balance`) - 成員可查看自己的貨幣餘額
- **設定貨幣** (`/currency-config`) - 管理員可自訂貨幣名稱和圖標
- **調整餘額** (`/adjust-balance`) - 管理員可增減成員的貨幣餘額

詳細使用說明請參考 [快速入門指南](specs/001-discord-currency-system/quickstart.md)。

## 快速開始

### 前置需求

- Java 17+
- Maven 3.8+
- Docker 和 Docker Compose
- Discord Bot Token

### 環境變數

本專案支援三種設定方式（優先順序由高到低）：
1. **系統環境變數** - 最高優先權
2. **`.env` 檔案** - 專案根目錄
3. **`application.properties`** - 內建設定檔

建議在本機開發時使用 `.env` 檔案：

```bash
# 複製範本檔案
cp .env.example .env

# 編輯 .env 檔案，填入你的設定
nano .env
```

| 變數名稱 | 說明 | 必要 | 預設值 |
|----------|------|:----:|--------|
| `DISCORD_BOT_TOKEN` | Discord 機器人 Token | ✅ | - |
| `DB_URL` | PostgreSQL 連線 URL | - | `jdbc:postgresql://localhost:5432/currency_bot` |
| `DB_USERNAME` | 資料庫使用者名稱 | - | `postgres` |
| `DB_PASSWORD` | 資料庫密碼 | - | `postgres` |
| `DB_POOL_MAX_SIZE` | 連線池最大數量 | - | `10` |
| `DB_POOL_MIN_IDLE` | 最小空閒連線 | - | `2` |
| `DB_POOL_CONNECTION_TIMEOUT` | 連線逾時（毫秒） | - | `30000` |
| `DB_POOL_IDLE_TIMEOUT` | 空閒逾時（毫秒） | - | `600000` |
| `DB_POOL_MAX_LIFETIME` | 最大連線生存期（毫秒） | - | `1800000` |

### 使用 Docker Compose 啟動

1. 設定環境變數：
```bash
export DISCORD_BOT_TOKEN="your-token-here"
```

2. 啟動服務：
```bash
make docker-up
```

3. 查看日誌：
```bash
make docker-logs
```

4. 停止服務：
```bash
make docker-down
```

### 本地開發

1. 啟動 PostgreSQL：
```bash
make db-up
```

2. 設定環境變數：
```bash
export DISCORD_BOT_TOKEN="your-token-here"
export DB_URL="jdbc:postgresql://localhost:5432/currency_bot"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
```

3. 建置專案：
```bash
make build
```

4. 執行機器人：
```bash
make run
```

## 開發指令

| 指令 | 說明 |
|------|------|
| `make build` | 建置專案（跳過測試） |
| `make test` | 執行單元測試 |
| `make test-integration` | 執行所有測試（含整合測試） |
| `make clean` | 清除建置產物 |
| `make run` | 本地執行機器人 |
| `make docker-build` | 建置 Docker 映像 |
| `make docker-up` | 啟動 Docker 服務 |
| `make docker-down` | 停止 Docker 服務 |
| `make docker-logs` | 查看 Docker 日誌 |
| `make db-up` | 僅啟動 PostgreSQL |
| `make db-down` | 停止 PostgreSQL |
| `make dev` | 開發環境設定（啟動資料庫） |
| `make help` | 顯示所有可用指令 |

## 專案結構

```
LTDJMS/
├── src/
│   ├── main/
│   │   ├── java/ltdjms/discord/
│   │   │   ├── currency/           # 貨幣系統模組
│   │   │   │   ├── bot/            # 機器人核心
│   │   │   │   ├── commands/       # 指令處理器
│   │   │   │   ├── domain/         # 領域模型
│   │   │   │   ├── persistence/    # 資料存取層
│   │   │   │   └── services/       # 業務邏輯層
│   │   │   └── shared/             # 共用元件
│   │   └── resources/
│   │       ├── db/schema.sql       # 資料庫結構
│   │       ├── application.properties
│   │       └── logback.xml         # 日誌設定
│   └── test/                       # 測試程式碼
├── specs/                          # 功能規格文件
├── docker-compose.yml
├── Dockerfile
├── Makefile
└── pom.xml
```

## 設定檔說明

### application.properties

應用程式設定檔，可用環境變數覆蓋：

```properties
# Discord Bot Token（建議使用環境變數）
discord.bot.token=${DISCORD_BOT_TOKEN:}

# 資料庫設定
db.url=${DB_URL:jdbc:postgresql://localhost:5432/currency_bot}
db.username=${DB_USERNAME:postgres}
db.password=${DB_PASSWORD:postgres}

# 連線池設定
db.pool.size=10
db.pool.name=CurrencyBotPool
```

### logback.xml

日誌設定檔，預設輸出至控制台：

- **應用程式日誌** (`ltdjms.discord.currency.*`) - INFO 層級
- **JDA 日誌** (`net.dv8tion.jda`) - INFO 層級
- **HikariCP 日誌** (`com.zaxxer.hikari`) - INFO 層級
- **根日誌** - WARN 層級

如需 JSON 格式日誌（用於容器環境），可修改 logback.xml 使用 `JSON_CONSOLE` appender。

## 資料庫

### 資料表

#### guild_currency_config
儲存每個伺服器的貨幣設定。

| 欄位 | 類型 | 說明 |
|------|------|------|
| guild_id | BIGINT | Discord 伺服器 ID (主鍵) |
| currency_name | VARCHAR(50) | 貨幣名稱 |
| currency_icon | VARCHAR(32) | 貨幣圖標/標籤（支援 emoji 和短文字，最多 32 字元） |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

#### member_currency_account
儲存每個成員在各伺服器的貨幣帳戶。

| 欄位 | 類型 | 說明 |
|------|------|------|
| guild_id | BIGINT | Discord 伺服器 ID (主鍵之一) |
| user_id | BIGINT | Discord 使用者 ID (主鍵之一) |
| balance | BIGINT | 餘額（不可為負） |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 更新時間 |

## 限制

- 單次調整金額上限：1,000,000（100萬）
- 餘額不可為負數
- 每個伺服器獨立管理貨幣設定和餘額

## 相關文件

- [功能規格](specs/001-discord-currency-system/spec.md)
- [快速入門](specs/001-discord-currency-system/quickstart.md)
- [資料模型](specs/001-discord-currency-system/data-model.md)
