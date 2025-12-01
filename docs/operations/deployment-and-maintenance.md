# 運維指南：部署與維護

本文件說明如何使用 Docker Compose 與 Make 指令部署、監控與維護 LTDJMS Discord Bot，並解釋啟動時自動 schema migration 的注意事項。

## 1. 部署模式概觀

LTDJMS 常見的部署模式：

1. **本機開發／測試**：使用 Docker Compose 一次啟動 Bot 與 PostgreSQL。
2. **測試環境（Staging）**：在 CI/CD pipeline 中建置映像，部署到共用伺服器或容器平台。
3. **正式環境（Production）**：與測試環境類似，但使用獨立的資料庫與更嚴謹的監控。

本文件以「使用專案內建的 `Makefile` + `docker-compose.yml`」為主要示例。

## 2. 使用 Make + Docker Compose 的常用指令

在專案根目錄執行：

```bash
# 建置 Docker 映像（通常在程式碼更新後執行）
make update

# 啟動服務（不強制重建映像）
make start

# 使用 layer cache 建置並啟動（開發中常用）
make start-dev

# 查看容器日誌
make logs

# 停止所有服務
make stop
```

若只需要資料庫（例如本機開發時手動執行 Bot）：

```bash
make db-up     # 啟動 PostgreSQL 容器
make db-down   # 停止 PostgreSQL 容器
```

## 3. 建置與發布流程建議

以下是一個簡化、可套用於測試與正式環境的流程範例：

1. **建置映像**

   在 CI 或本機：

   ```bash
   make update
   ```

   此指令會呼叫 `docker compose build`，利用 Docker layer cache 加速建置並產生新的 Bot 映像。

2. **設定環境變數**

   在實際部署環境中配置：

   - `DISCORD_BOT_TOKEN`
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - 以及需要的連線池參數

3. **啟動／更新服務**

   ```bash
   make start       # 或在開發環境使用 make start-dev
   ```

4. **檢查日誌**

   ```bash
   make logs
   ```

   確認：

   - Bot 成功連線 Discord。
   - 資料庫 schema migration 無錯誤（見下一節）。

## 4. 啟動時的自動 schema migration

在 `DiscordCurrencyBot` 啟動時，會透過：

- `DatabaseSchemaMigrator.forDefaultSchema().migrate(dataSource)`

執行自動 schema migration。其行為大致為：

- 比對實際資料庫 schema 與 `src/main/resources/db/schema.sql`。
- 若差異屬於**非破壞性變更**，嘗試自動套用，例如：
  - 新增資料表。
  - 在既有表中新增欄位（允許 `NULL` 或具 `DEFAULT` 的 `NOT NULL` 欄位）。
  - 建立缺漏的索引、触發器或函式（配合 `IF NOT EXISTS` / `CREATE OR REPLACE`）。
- 若偵測到可能破壞既有資料的變更，則視為**破壞性變更**並中止啟動，例如：
  - 欄位型別變更。
  - 刪除或重新命名欄位。
  - 新增無預設值的 `NOT NULL` 欄位。

遇到破壞性變更時：

- Bot 會丟出 `SchemaMigrationException`（或等價錯誤），並在日誌中詳述差異。
- 不會對資料庫做出任何修改。
- Bot 啟動失敗，避免在不一致的 schema 上運作。

### 4.1 建議的破壞性變更處理流程

1. 在本地或測試環境先修改 `schema.sql`，並嘗試啟動，觀察 Bot 的 schema 差異報告。
2. 根據報告撰寫手動 migration SQL（例如使用 Flyway、Liquibase 或手工腳本）。
3. 在維護時段：
   - 將 Bot 服務停止或切換到維護模式。
   - 對目標資料庫執行 migration SQL。
4. 再次啟動 Bot：
   - 確認 `DatabaseSchemaMigrator` 不再報告破壞性變更。

## 5. 健康檢查與監控建議

LTDJMS 本身未內建 HTTP 健康檢查端點，但你可以藉由以下方式監控：

- **容器層級**：
  - 使用 Docker / 容器平台的 restart policy（例如 `restart: always`）。
  - 監控容器是否持續重啟，若有異常重啟次數增加，需檢查日誌。

- **Discord 層級**：
  - 觀察 Bot 是否在線（online）且 slash commands 是否可用。
  - 透過簡單的監控 Bot（例如另一個監控 Bot 或外部服務）定期呼叫 `/balance` 或自訂指令檢查回應。

- **資料庫層級**：
  - 監控 PostgreSQL 的連線數、慢查詢與儲存空間。

## 6. 日誌與問題排查

### 6.1 查看日誌

```bash
make logs
```

常見要注意的訊息：

- JDA 連線錯誤（Token 無效、權限不足等）。
- 資料庫連線失敗或 schema migration 錯誤。
- 服務層拋出的 `DomainError`（通常以警告或錯誤等級記錄）。

### 6.2 本機重現問題

若在正式環境遇到問題，建議：

1. 將相同版本的程式碼與 `schema.sql` 拉到本機。
2. 使用 Docker Compose 啟動一個與正式環境相近的 PostgreSQL。
3. 匯入相關資料（若可能，使用部分匿名化的資料）。
4. 執行問題指令（如 `/dice-game-2`、`/admin-panel`），觀察行為與日誌。

## 7. 升級與回滾建議

### 7.1 升級

1. 在 Git 上切換到新版本（或拉取最新版本）。
2. 執行 `make update` 建置新映像。
3. 執行 `make start` 或由 Orchestrator 滾動更新服務。
4. 監控啟動日誌與行為。

### 7.2 回滾

若新版本出現問題：

1. 回到先前穩定版本的程式碼。
2. 重新建置映像（或直接切換到舊映像標籤）。
3. 重新啟動服務。

> 重要：在 schema 有破壞性變更時，回滾版本可能需要同時回滾資料庫 schema。建議搭配專用 migration 工具管理版本化的 schema 變更。

## 8. 小結

- 使用 `Makefile` + `docker-compose.yml` 可以快速啟動與管理 Bot 與 PostgreSQL。
- 啟動時自動 schema migration 可以處理大部分非破壞性變更，但對破壞性變更會保守地中止啟動。
- 在正式環境中，建議將 Token 與資料庫密碼放在安全的秘密管理系統，而不是 `.env` 檔案。
- 發生問題時，優先查看容器日誌與 Discord Bot 線上狀態，再回推到資料庫與設定層面做排查。

