# 快速入門：安裝與啟動 LTDJMS Discord Bot

本文件說明如何從 0 開始安裝、建置、設定並啟動 LTDJMS Discord Bot，並將它加入你的 Discord 伺服器。

## 1. 前置需求

- **Java 17** 或以上
- **Maven 3.8+**
- **Docker** 與 **Docker Compose**（建議在本機開發與測試時使用）
- 一個擁有伺服器管理權限的 **Discord 帳號**

## 2. 取得與建置專案

```bash
git clone <repository-url>
cd LTDJMS
```

### 2.1 建置專案

```bash
# 使用 Make（推薦）
make build

# 或直接使用 Maven
mvn clean package -DskipTests
```

### 2.2 執行測試（可選但建議）

```bash
# 執行單元測試
make test

# 執行所有測試（包含整合測試）
make test-integration
```

> 提醒：本專案的測試套件涵蓋貨幣系統、遊戲代幣與面板等功能，建議在修改程式碼前先確定測試能在你的環境中順利通過。

## 3. 建立 Discord 應用程式與 Bot

### 3.1 建立應用程式與 Bot 帳號

1. 前往 Discord Developer Portal：<https://discord.com/developers/applications>
2. 點選 **New Application** 建立新應用程式，輸入名稱（例如：`LTDJMS Bot`）。
3. 在左側選單中進入 **Bot** 分頁，點選 **Add Bot** 建立機器人帳號。
4. 在 **TOKEN** 區塊點選 **Reset Token**，並複製新的 Bot Token。

> 安全提醒：**請勿**將 Token 提交到版本控制系統或公開分享。建議只透過環境變數或 `.env` 檔案存放。

### 3.2 設定 Bot Intents

LTDJMS 主要使用 slash commands 與互動元件，對於一般使用情境，不需要啟用特別的 Privileged Gateway Intents。  
如果你打算擴充功能使用成員事件，則可在 **Bot** 頁面視需要啟用：

- `SERVER MEMBERS INTENT`

（目前程式碼啟動 JDA 時僅使用非特權 intents，以避免因權限不足而無法連線。）

### 3.3 產生邀請網址並加入伺服器

1. 在 Developer Portal 中選擇你的應用程式。
2. 前往 **OAuth2** → **URL Generator**。
3. 在 **SCOPES** 勾選：
   - `bot`
   - `applications.commands`
4. 在 **BOT PERMISSIONS** 勾選至少：
   - `Send Messages`
   - `Use Slash Commands`
5. 複製產生的邀請 URL，在瀏覽器中開啟並選擇要加入的伺服器。

完成後，你應該能在 Discord 伺服器成員列表中看到新的 Bot。

## 4. 設定環境變數與 `.env`

LTDJMS 透過 `EnvironmentConfig` 載入設定，優先順序如下：

1. 系統環境變數
2. 專案根目錄的 `.env` 檔案
3. `application.conf` / `application.properties`
4. 內建預設值

### 4.1 使用 `.env` 檔案（本機開發推薦）

在專案根目錄建立 `.env` 檔案：

```bash
cp .env.example .env   # 如果你已經有範本檔
```

編輯 `.env`：

```dotenv
DISCORD_BOT_TOKEN=your-bot-token-here
DB_URL=jdbc:postgresql://localhost:5432/currency_bot
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

### 4.2 直接使用環境變數（CI / Docker Compose 常見）

```bash
export DISCORD_BOT_TOKEN="your-bot-token-here"
export DB_URL="jdbc:postgresql://localhost:5432/currency_bot"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"
```

其他可選的連線池參數（如不設定則使用預設值）：

- `DB_POOL_MAX_SIZE`
- `DB_POOL_MIN_IDLE`
- `DB_POOL_CONNECTION_TIMEOUT`
- `DB_POOL_IDLE_TIMEOUT`
- `DB_POOL_MAX_LIFETIME`

詳細說明可參考 `docs/development/configuration.md`。

## 5. 使用 Docker Compose 啟動（推薦）

在專案根目錄以已設定好的環境變數或 `.env` 為前提，執行：

1. 啟動開發用容器（建議）：

   ```bash
   make start-dev
   ```

   此指令會：

   - 透過 Docker layer cache 建置映像（只重建有變更的層）
   - 啟動 PostgreSQL 與 Bot 容器
   - 在 Bot 啟動時自動比對並套用與 `src/main/resources/db/schema.sql` 一致的**非破壞性** schema 變更

2. 如只需重新啟動既有容器而不重建映像：

   ```bash
   make start
   ```

3. 查看日誌：

   ```bash
   make logs
   ```

4. 停止服務：

   ```bash
   make stop
   ```

> 若啟動時偵測到破壞性 schema 差異，Bot 會停止啟動並在日誌中輸出 `SchemaMigrationException`，請依日誌指示先手動處理資料庫遷移後再重啟。

## 6. 不使用 Docker 的本地開發流程

如果你希望直接在本機安裝的 PostgreSQL 上開發，可以使用：

1. 啟動 PostgreSQL（使用 docker-compose）：

   ```bash
   make db-up
   ```

2. 設定環境變數（或 `.env`）：

   ```bash
   export DISCORD_BOT_TOKEN="your-bot-token-here"
   export DB_URL="jdbc:postgresql://localhost:5432/currency_bot"
   export DB_USERNAME="postgres"
   export DB_PASSWORD="postgres"
   ```

3. 建置與執行：

   ```bash
   make build
   make run
   ```

4. 開發完成後可以關閉資料庫容器：

   ```bash
   make db-down
   ```

## 7. 在 Discord 上驗證 Bot 是否正常運作

當 Bot 成功啟動並加入伺服器後，你可以透過以下步驟驗證：

1. 在伺服器任一文字頻道輸入：

   ```text
   /balance
   ```

   - 第一次使用時，系統會為你建立帳戶並顯示初始餘額（預設為 0）。

2. 以具管理員權限的帳號輸入：

   ```text
   /currency-config name:金幣 icon:💰
   ```

   - 你應該會收到包含新貨幣名稱與圖示的成功訊息。

3. 以管理員身分輸入：

   ```text
   /adjust-balance mode:add member:@某位成員 amount:100
   ```

   - 對應成員的餘額應該會增加 100，並在訊息中顯示調整前後的餘額。

4. 測試遊戲代幣與小遊戲：

   - 先給自己一些遊戲代幣：

     ```text
     /game-token-adjust member:@自己 amount:10
     ```

   - 再嘗試骰子遊戲 1：

     ```text
     /dice-game-1
     ```

5. 查看個人面板：

   ```text
   /user-panel
   ```

   - 應該會看到你的貨幣餘額與遊戲代幣餘額，並有查看遊戲代幣流水的按鈕。

若以上流程皆正常，代表 Bot 已成功安裝並運作。

## 8. 下一步

- 想了解所有可用指令與參數：請閱讀  
  `docs/api/slash-commands.md`
- 想理解系統架構與資料模型：請閱讀  
  `docs/architecture/overview.md` 與 `docs/architecture/data-model.md`
- 想擴充功能或調整行為：建議先閱讀  
  `docs/modules/currency-system.md`、`docs/modules/game-tokens-and-games.md` 與 `docs/modules/panels.md`

