# 故障排除指南

本文件提供 LTDJMS Discord Bot 常見問題的診斷與解決方法。若你遇到問題，請先檢查此指南。若問題仍未解決，請參考相關文件或建立議題。

> 補充說明：目前正式啟動入口以根目錄 `README.md` 與 `docs/getting-started.md` 為準；本機直跑請使用 `java -jar target/ltdjms-*.jar`。

## 快速檢查清單

在深入診斷前，請先確認以下基本項目：

- [ ] **Discord Bot Token** 已正確設定（環境變數或 `.env` 檔案）
- [ ] **資料庫連線** 正常（PostgreSQL 服務正在運行，連線資訊正確）
- [ ] **網路連線** 正常（Bot 能存取 Discord API）
- [ ] **權限設定** 正確（Bot 有適當的 Discord 權限）
- [ ] **AI 服務配置** 已正確設定（若使用 AI Chat 功能）
- [ ] **最新版本** 已使用最新程式碼或 Docker 映像

## 常見問題與解決方案

### 1. Bot 無法啟動（立即結束）

#### 症狀
- 執行 `make start`、`make start-dev` 或 `java -jar target/ltdjms-*.jar` 後程式立即結束
- 容器啟動後立即退出

#### 可能原因與解決方案

**A. Discord Bot Token 未設定或無效**
```bash
# 檢查環境變數
echo $DISCORD_BOT_TOKEN

# 檢查 .env 檔案
cat .env | grep DISCORD_BOT_TOKEN
```

**解決方案：**
1. 到 [Discord Developer Portal](https://discord.com/developers/applications) 重新產生 Token
2. 更新 `.env` 檔案或環境變數
3. 重新啟動 Bot

**B. 資料庫連線失敗**
```bash
# 檢查 PostgreSQL 是否運行
docker ps | grep postgres

# 測試資料庫連線
docker exec ltdjms-postgres-1 psql -U postgres -d currency_bot -c "SELECT 1;"
```

**解決方案：**
1. 確認 PostgreSQL 容器已啟動：`make db-up`
2. 檢查連線設定（`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`）
3. 確認防火牆未封鎖連接埠（預設 5432）

**C. Schema 遷移失敗（SchemaMigrationException）**
```
ERROR: SchemaMigrationException: Detected potentially destructive schema differences
- Table "guild_currency_config": column "currency_icon" type mismatch
  Expected: VARCHAR(64), Actual: TEXT
```

**解決方案：**
1. 查看完整錯誤訊息，確認具體差異
2. 規劃手動遷移 SQL 腳本
3. 在維護時段執行遷移
4. 詳細流程參考：[資料庫遷移處理](#資料庫遷移處理)

### 2. Bot 無法連線 Discord

#### 症狀
- Bot 程式運行中，但 Discord 中顯示為離線
- 日誌中出現連線錯誤或逾時

#### 可能原因與解決方案

**A. 網路連線問題**
```bash
# 測試 Discord API 連線
curl -I https://discord.com/api/v10
```

**解決方案：**
1. 確認網路設定（代理、防火牆）
2. 檢查 Docker 網路設定（如使用容器）

**B. Bot Token 權限不足**
```
[ERROR] LoginException: Invalid token provided
```

**解決方案：**
1. 確認 Token 對應的應用程式與 Bot 正確
2. 檢查 Bot 是否在 Developer Portal 中被停用
3. 重新產生 Token 並更新設定

**C. Intents 設定不正確**
```
[WARN] JDA: Some events may not be received because of missing intents
```

**解決方案：**
1. 確認 Discord Developer Portal 中的 Bot Intents 設定
2. 檢查 `DiscordCurrencyBot.java` 中的 `createLight` 方法設定
3. 目前版本使用非特權 Intents，通常不需調整

### 3. Slash 指令無法使用

#### 症狀
- Bot 在線，但輸入 `/` 後看不到指令
- 指令顯示但執行時出現錯誤

#### 可能原因與解決方案

**A. 指令未註冊或同步失敗**
```
[INFO] SlashCommandListener: Registered 5 slash commands
```

**解決方案：**
1. 確認 Bot 啟動日誌顯示指令註冊成功
2. 檢查 Bot 是否有 `applications.commands` OAuth2 權限
3. 重新邀請 Bot（使用包含 `applications.commands` 範圍的邀請連結）

**B. 權限不足**
```
You don't have permission to use this command.
```

**解決方案：**
1. 確認使用者擁有指令所需權限（如管理員指令需 Administrator 權限）
2. 檢查 Discord 伺服器角色設定

**C. 指令執行錯誤（DomainError）**
```
❌ Insufficient balance
Your current balance: 100 coins
Required: 500 coins
```

**解決方案：**
1. 此為業務邏輯錯誤，非系統錯誤
2. 按照錯誤訊息指示操作（如補充餘額）
3. 詳細錯誤類型參考：[業務錯誤參考](#業務錯誤參考)

### 4. 資料庫相關問題

#### 症狀
- 指令執行時出現資料庫錯誤
- 資料不一致或遺失

#### 可能原因與解決方案

**A. 連線池耗盡**
```
[HikariPool-1] - Connection is not available, request timed out after 30000ms
```

**解決方案：**
1. 調整連線池設定（`DB_POOL_MAX_SIZE`、`DB_POOL_MIN_IDLE`）
2. 檢查是否有連線洩漏（長時間未關閉的連線）
3. 重啟服務暫時緩解

**B. 資料庫鎖定或死結**
```
Deadlock found when trying to get lock
```

**解決方案：**
1. 重試操作（系統會自動重試輕量級死結）
2. 優化交易範圍，減少鎖定時間
3. 檢查索引設定，避免全表掃描

**C. 資料不一致**
- 餘額為負數（應為非負）
- 重複的資料記錄

**解決方案：**
1. 檢查領域模型的約束條件
2. 執行資料驗證腳本
3. 必要時手動修正資料

### 5. 效能問題

#### 症狀
- 指令回應緩慢
- 高負載時系統不穩定

#### 可能原因與解決方案

**A. 資料庫查詢緩慢**
```bash
# 檢查資料庫慢查詢
docker exec ltdjms-postgres-1 psql -U postgres -d currency_bot -c "
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
"
```

**解決方案：**
1. 為常用查詢欄位新增索引
2. 優化複雜查詢，避免 N+1 問題
3. 使用 EXPLAIN ANALYZE 分析查詢計畫

**B. Discord API 速率限制**
```
[WARN] JDA: Hit rate limit for endpoint /channels/{id}/messages
```

**解決方案：**
1. 降低訊息發送頻率
2. 使用批次操作（如 Embed 合併多筆資訊）
3. 遵循 Discord API 最佳實踐

**C. 資源不足**
- 記憶體不足（OOM）
- CPU 使用率過高

**解決方案：**
1. 調整 JVM 參數（`-Xmx`、`-Xms`）
2. 監控容器資源使用情況
3. 水平擴展（多個 Bot 實例）

### 6. AI Chat 功能問題（V010 新增）

#### 症狀
- 提及機器人後沒有回應
- AI 回應顯示錯誤訊息
- Bot 啟動時提示 AI 配置錯誤

#### 可能原因與解決方案

**A. AI 服務配置未設定或無效**

```
ERROR IllegalStateException: AI_SERVICE_API_KEY is required but not configured
```

**解決方案：**
```bash
# 檢查 AI 服務配置
cat .env | grep AI_SERVICE

# 確認以下變數已設定：
# - AI_SERVICE_BASE_URL
# - AI_SERVICE_API_KEY
```

**B. AI 服務認證失敗**

使用者看到：`:x: AI 服務認證失敗，請聯絡管理員`

**解決方案：**
1. 確認 `AI_SERVICE_API_KEY` 正確
2. 檢查 API 金鑰是否已過期
3. 若使用 OpenAI，確認帳戶餘額充足
4. 測試 API 連線：
   ```bash
   curl -X POST https://api.openai.com/v1/chat/completions \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $AI_SERVICE_API_KEY" \
     -d '{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"test"}]}'
   ```

**C. AI 服務速率限制**

使用者看到：`:timer: AI 服務暫時忙碌，請稍後再試`

**解決方案：**
1. 等待一段時間後重試
2. 降低 AI 溫度（`AI_SERVICE_TEMPERATURE`）以減少複雜度
3. 改用較小或較快的模型，並縮短提示詞內容
4. 考慮升級 AI 服務方案

**D. AI 服務連線逾時**

使用者看到：`:hourglass: AI 服務暫時無法使用`

**解決方案：**
1. 檢查網路連線是否正常
2. 調整連線逾時設定（`AI_SERVICE_TIMEOUT_SECONDS`），預設 30 秒
3. 確認 AI 服務端點是否可達：
   ```bash
   curl -I $AI_SERVICE_BASE_URL
   ```

**E. AI 服務回應空內容**

使用者看到：`:question: AI 沒有產生回應`

**解決方案：**
1. 嘗試重新發送訊息
2. 調整 AI 溫度參數
3. 檢查 AI 服務日誌確認問題

**F. 機器人提及無回應**

**解決方案：**
1. 確認機器人在該頻道有「傳送訊息」權限
2. 檢查訊息格式是否正確（如 `@BotName 你好`）
3. 查看日誌確認事件是否被觸發：
   ```bash
   make logs | grep -i aichat
   ```

**G. AI 回應速度慢**

**解決方案：**
1. 改用較小或較快的模型，並縮短提示詞內容
2. 選擇更快的模型（如 `gpt-3.5-turbo` 而非 `gpt-4`）
3. 檢查 AI 服務的地理位置延遲
4. 注意：`AI_SERVICE_TIMEOUT_SECONDS` 只影響連線時間，不限制推理時間

#### AI Chat 故障排除檢查清單

```bash
# 1. 確認 AI 服務配置
echo "Base URL: $AI_SERVICE_BASE_URL"
echo "API Key: ${AI_SERVICE_API_KEY:0:10}..."  # 僅顯示前 10 字元

# 2. 測試 AI 服務連線
curl -X POST $AI_SERVICE_BASE_URL/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AI_SERVICE_API_KEY" \
  -d '{"model":"'"$AI_SERVICE_MODEL"'","messages":[{"role":"user","content":"Hello"}],"max_tokens":10}'

# 3. 檢查 Bot 日誌
make logs | grep -i aichat

# 4. 檢查 AI Chat 事件
make logs | grep "AI chat request"

# 5. 驗證配置參數範圍
# - Temperature: 0.0 - 2.0
# - Max Tokens: 1 - 4096
# - 連線逾時秒數: 1 - 120
```

#### AI Chat 常見錯誤訊息對照表

| 使用者看到訊息 | 原因 | 解決方案 |
|--------------|------|---------|
| `:x: AI 服務認證失敗，請聯絡管理員` | HTTP 401 | 更新有效的 API 金鑰 |
| `:timer: AI 服務暫時忙碌，請稍後再試` | HTTP 429 | 等待後重試或升級方案 |
| `:hourglass: AI 服務連線逾時，請稍後再試` | 連線逾時 | 檢查網路或調整連線逾時 |
| `:warning: AI 回應格式錯誤` | JSON 解析失敗 | 檢查 AI 服務回應格式 |
| `:question: AI 沒有產生回應` | 空回應 | 重新發送或調整參數 |

## 資料庫遷移處理

### 自動遷移流程
Bot 啟動時會自動執行 Flyway 遷移：
1. 檢查 `src/main/resources/db/migration/` 下的 pending migrations
2. 依版本順序執行遷移腳本
3. 記錄遷移歷史於 `flyway_schema_history` 表

### 破壞性變更處理
當偵測到破壞性變更時，Bot 會中止啟動並輸出 `SchemaMigrationException`。

**處理步驟：**
1. **分析差異**：閱讀錯誤訊息，確認具體差異
2. **規劃遷移**：撰寫手動遷移 SQL 腳本
3. **執行遷移**：在維護時段執行，例如：
   ```sql
   -- 範例：安全地變更欄位型別
   BEGIN;
   ALTER TABLE guild_currency_config
   ADD COLUMN currency_icon_new VARCHAR(64);

   UPDATE guild_currency_config
   SET currency_icon_new = currency_icon;

   ALTER TABLE guild_currency_config
   DROP COLUMN currency_icon;

   ALTER TABLE guild_currency_config
   RENAME COLUMN currency_icon_new TO currency_icon;
   COMMIT;
   ```
4. **驗證一致性**：確認資料庫 schema 與 `schema.sql` 一致
5. **重新啟動**：執行 `make start-dev` 重新啟動 Bot

### 遷移最佳實踐
- **非破壞性優先**：盡量使用新增欄位、新增表格等非破壞性變更
- **資料備份**：執行遷移前先備份資料庫
- **測試環境驗證**：先在測試環境驗證遷移腳本
- **回滾計畫**：準備回滾腳本以應對遷移失敗

## 業務錯誤參考

### 常見 DomainError 類型

| 錯誤類別 | 原因 | 解決方案 |
|---------|------|---------|
| `INVALID_INPUT` | 輸入參數無效 | 檢查參數格式與範圍 |
| `INSUFFICIENT_BALANCE` | 貨幣餘額不足 | 補充貨幣餘額 |
| `INSUFFICIENT_TOKENS` | 遊戲代幣不足 | 補充遊戲代幣 |
| `PERSISTENCE_FAILURE` | 資料庫操作失敗 | 檢查資料庫連線與狀態 |
| `UNEXPECTED_FAILURE` | 未預期的系統錯誤 | 查看日誌詳細錯誤 |

### 錯誤處理流程
1. **前端顯示**：`BotErrorHandler` 將 `DomainError` 轉為使用者易懂的訊息
2. **日誌記錄**：同時記錄詳細錯誤資訊於日誌中
3. **後續處理**：根據錯誤類型決定後續流程（如重試、終止等）

## 日誌分析

### 日誌層級說明
- **ERROR**：需要立即處理的錯誤（如連線失敗、遷移失敗）
- **WARN**：潛在問題（如速率限制、暫時性錯誤）
- **INFO**：正常操作記錄（如指令執行、啟動完成）
- **DEBUG**：詳細除錯資訊（需手動啟用）

### 關鍵日誌模式
```log
# 正常啟動
INFO  DiscordCurrencyBot - Bot started successfully

# 指令執行
INFO  SlashCommandMetrics - /user-panel completed in 150ms (success)

# 錯誤情況
ERROR BotErrorHandler - Command failed: /dice-game-1, error: INSUFFICIENT_TOKENS
```

### 日誌收集建議
- **容器環境**：將日誌輸出至 stdout/stderr，由 Docker 收集
- **生產環境**：使用集中式日誌系統（如 ELK、Loki）
- **除錯時**：啟用 DEBUG 層級日誌 `LOG_LEVEL=DEBUG`

## 監控與警報

### 關鍵指標
1. **上線狀態**：Bot 是否在線
2. **指令延遲**：各指令平均處理時間
3. **錯誤率**：指令失敗比例
4. **資料庫連線**：活躍連線數、等待時間
5. **資源使用**：記憶體、CPU 使用率

### 簡易監控腳本
```bash
#!/bin/bash
# 檢查 Bot 是否在線
if curl -s http://localhost:8080/health > /dev/null; then
    echo "Bot is healthy"
else
    echo "Bot is down" | mail -s "LTDJMS Alert" admin@example.com
fi
```

## 尋求進一步協助

若問題仍無法解決：

1. **檢查文件**：
   - [快速入門指南](../getting-started/quickstart.md)
   - [系統架構](../architecture/overview.md)
   - [部署指南](deployment-and-maintenance.md)

2. **查看原始碼**：相關程式碼位置通常會在錯誤訊息或文件中提到

3. **建立議題**：提供以下資訊：
   - 錯誤訊息與完整日誌
   - 重現步驟
   - 環境資訊（OS、Java 版本、Docker 版本等）
   - 已嘗試的解決方案

---

**提示**：定期備份資料庫與設定檔是預防資料遺失的最佳實踐。
