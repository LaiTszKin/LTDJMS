# AGENTS.md

## Core Project Purpose

LTDJMS 的目標是在單一 Discord bot 內承載 guild 的經濟互動、商品交易、護航協作與 AI 頻道治理，讓營運流程不需要在多個外部系統之間切換。

## Project Architecture

- 主要啟動入口：`src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
- Discord 互動入口：`SlashCommandListener`、`AIChatMentionListener`、面板 / 按鈕 / select menu / modal handlers、`EcpayCallbackHttpServer`
- 分層慣例：各模組大致依 `domain/`、`persistence/`、`services/`、`commands/` 拆責任
- 主要模組：
  - `currency/`：guild 貨幣設定、餘額、交易
  - `gametoken/`：遊戲代幣、骰子遊戲、代幣交易
  - `panel/`：`/user-panel`、`/admin-panel`、session 與互動畫面
  - `product/`、`redemption/`、`shop/`：商品、兌換碼、商店、貨幣購買、法幣訂單、履約與通知
  - `dispatch/`：護航派單、定價、完單、售後
  - `aichat/`、`aiagent/`：AI 頻道限制、Agent 頻道配置、對話記憶、工具執行與審計
  - `markdown/`：AI 回應 Markdown 驗證、切段與自動修正
  - `discord/`：Discord 抽象層、adapter、embed builder、session abstraction
  - `shared/`：設定、資料庫、快取、事件、DI、共用型別
- 依賴注入：Dagger 2 編譯期 DI，由 `AppComponent` 與各 `*Module` 組裝
- 事件系統：`DomainEventPublisher` 透過 `EventModule` 與 Dagger multibinding 收集 listeners，採同步分發與例外隔離
- 部署入口：`docker-compose.yml` 內建 PostgreSQL、Redis、bot 與 repo 管理的 Caddy HTTPS ingress；`.env.example` 與 `scripts/setup-env.sh` / `scripts/sync-env.sh` 共同維護 operator 設定流程
- 主要資料與整合：PostgreSQL、Redis、Discord API、OpenAI-compatible AI provider、ECPay、Caddy

## Core Business Flow

這個專案讓 Discord guild 可以在同一個 bot 內完成經濟互動、商品交易、派單協作與 AI 輔助流程。

- Users can 查詢自己的 guild 貨幣、遊戲代幣、交易歷史與兌換歷史。
- Users can 透過 `/user-panel` 開啟個人面板並輸入兌換碼。
- Users can 玩 `/dice-game-1` 與 `/dice-game-2`，用遊戲代幣換取貨幣獎勵。
- Users can 瀏覽 `/shop` 商品並使用 guild 貨幣購買商品。
- Users can 對法幣商品建立綠界超商代碼訂單，並收到訂單編號與付款期限提醒。
- Users can 在付款完成後收到買家私訊通知，並由背景 worker 補做商品獎勵或護航相關通知。
- Users can 在購買有護航服務的商品後收到私訊，告知護航訂單已建立並等待處理。
- Users can 在允許頻道提及 Bot 取得 AI 回應。
- Users can 在啟用 Agent 的頻道讓 AI Agent 操作頻道、類別、角色、權限與訊息。
- Admins can 用 `/currency-config` 設定 guild 貨幣名稱與圖示。
- Admins can 用 `/admin-panel` 調整成員餘額、遊戲代幣與遊戲設定。
- Admins can 用 `/admin-panel` 建立、編輯與管理商品。
- Admins can 產生、管理與追蹤兌換碼。
- Admins can 設定 AI 允許頻道 / 類別白名單。
- Admins can 設定 AI Agent 啟用頻道。
- Admins can 設定護航定價與售後人員名單。
- Admins can 用 `/dispatch-panel` 建立護航派單。
- Admins can 在護航商品被購買後收到私訊通知，含客戶資訊、訂單詳情與付款狀態。
- Escort staff can 在私訊流程中確認接單、推進完單並承接售後案件。
- Maintainers can 透過 Docker Compose 啟動 bot、PostgreSQL 與 Redis。
- Maintainers can 透過 `make setup-env` 互動式建立部署用 `.env`，並用 `make update-env` 同步新增欄位而不覆寫既有 secrets。
- Maintainers can 透過啟動期 Flyway migration 維護資料庫 schema。
- Maintainers can 透過日誌、測試與 callback server 排查 Discord、付款與 AI 流程問題。

## Common Commands

- `make build` — 打包專案並驗證可編譯（跳過測試）。
- `make test` — 執行單元測試。
- `make test-integration` — 執行 `mvn verify` 等級的整合驗證。
- `make verify` — clean 後跑完整驗證流程。
- `make format` — 用 Spotless 格式化程式碼。
- `make format-check` — 檢查格式是否符合規範。
- `make start-dev` — 建置並啟動 bot、PostgreSQL、Redis。
- `make logs` — 追蹤 Docker Compose 日誌。
- `make db-up` — 只啟動 PostgreSQL 供本機 JVM 使用。
- `make setup-env` — 互動式建立或更新部署用 `.env`。
- `make update-env` — 以 `.env.example` 非互動同步缺漏欄位並保留既有值。
- `java -jar target/ltdjms-*.jar` — 在完成 build 後本機直接啟動 bot。

## Code Style And Coding Conventions

- 使用 Java 17，沿用既有 package 結構與 DDD 分層，不要新增平行抽象。
- 優先重用既有 service、repository、facade、listener、handler，不要複製流程。
- handler 保持為 Discord 事件協調器；複雜畫面組裝優先放在 factory / builder / helper。
- 維持既有 Discord custom ID、modal ID、localization key、session 語意與互動流程相容性。
- 服務層優先使用 `Result<T, DomainError>` 表達失敗，不要直接丟通用例外。
- 快取失效、面板更新、Agent 同步等副作用應透過 `DomainEventPublisher` 與 listeners 串接，不要硬塞回主流程。
- 商品、付款、派單、售後這類有狀態轉移的流程，修改前先確認 repository 的 claim / idempotency 邏輯。
- 修改 schema 相關行為時，要一起檢查 Flyway migration、repository 映射與測試。
- 設定與預設值以 `EnvironmentConfig` 為準；文件若和程式碼衝突，應優先修正文檔。
- 專案文件以根目錄 `README.md` 與 `docs/*.md` 主文件為閱讀入口；深度文件屬補充參考。
