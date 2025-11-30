# Changelog

All notable changes to this project will be documented in this file.

## [0.2.0] - 2025-11-30

### Added
- 新增「遊戲代幣」系統：`game_token_account` 資料表、`GameTokenAccount` domain、`GameTokenService` 與 JDBC repository，並加入非負餘額與 `InsufficientTokensException` 檢查。
- 新增 `/game-token-adjust` 管理員斜線指令，可為成員加減遊戲代幣並回傳清楚的調整結果訊息。
- 新增骰子小遊戲 `dice-game-1`：`DiceGame1Service`、`/dice-game-1` 指令與 `DiceGame1Config` / `dice_game1_config` 設定，支援每局消耗遊戲代幣、發放高額貨幣獎勵與每伺服器獨立的代幣消耗設定。
- 新增 game-token 與 dice-game-1 的單元與整合測試，涵蓋 repository、service 與指令訊息格式。
- 新增 JaCoCo Maven plugin 與 `make coverage` / `make test-coverage` 目標，用於產生測試覆蓋率報告。

### Changed
- 調整整合測試基底 `PostgresIntegrationTestBase`，在每個測試案例前一併清理 `game_token_account` 與 `dice_game1_config` 資料表。
- 調整 Makefile 中 Docker 相關目標命名為 `update`、`start`、`start-dev`、`stop`、`logs` 與 `restart`，使本地開發與容器操作流程更一致。

## [0.1.1] - 2025-11-30

### Added
- 新增 `DatabaseSchemaMigrator` 與啟動時自動套用**非破壞性**資料庫 schema 遷移的機制，偵測破壞性變更時會中止啟動並丟出 `SchemaMigrationException`。
- 新增 `EmojiValidator` 介面與 `JdaEmojiValidator` / `NoOpEmojiValidator`，支援對 Discord 自訂 emoji 標記進行驗證，並在測試環境中以 no-op 驗證器隔離 JDA 依賴。
- 新增整合測試與測試用 SQL schema，用於驗證初次啟動、非破壞性欄位新增與破壞性欄位移除等情境下的遷移行為。

### Changed
- `/currency-config` 指令現在會從 Discord slash 指令欄位的 Mentions 中解析自訂 emoji，並以標準 `<:name:id>` / `<a:name:id>` 形式持久化，與手動輸入標記的行為一致。
- 將 `guild_currency_config.currency_icon` 最大長度從 32 擴大到 64，並更新對應領域模型與測試，以更好支援複合 emoji / 短文字標籤。
- 改善 Docker 開發流程，新增 `make docker-dev` 目標與相對應的 README 說明，利用 Maven 依賴快取與 Docker layer cache，加速重新建置與啟動。

### Fixed
- `EnvironmentConfigTest` 改為固定使用 `src/test/resources` 作為設定目錄，避免受本機 `.env` 影響，確保測試在不同開發環境下結果一致。

## [0.1.0] - 2025-11-30

### Added
- 新增基於 Maven 的 Java 17 Discord 貨幣機器人專案
- 實作伺服器貨幣系統：/balance、/currency-config、/adjust-balance 指令與對應服務層與持久化層
- 新增整合測試、契約測試、單元測試與效能測試，涵蓋貨幣指令與 PostgreSQL 整合
- 新增 Dockerfile、docker-compose、Makefile、.env 範本與相關 ignore 設定，支援本地與容器化部署

### Removed
- 移除不再使用的 .specify 腳本與模板
