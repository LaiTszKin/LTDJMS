# Developer Guide

## 1. Domain Concepts To Know

- Guild-scoped state: 貨幣設定、帳戶餘額、AI 限制、Agent 啟用狀態、派單售後人員與商品都以 guild 為邊界
- `Result<T, DomainError>`: 服務層以顯式結果物件回傳成功 / 失敗，而非大量丟出例外
- Event-driven updates: 面板刷新、快取失效、AI Agent 狀態同步依賴 `DomainEventPublisher` 的同步分發
- Product purchase split: 商品的「付款 / 扣款」與「獎勵發放 / 後端履約」是兩段式流程，法幣下單還會再拆成建單與付款回推兩階段
- Dispatch lifecycle: 派單有建立、護航者確認、客戶確認完單、售後申請、售後接案與結案等狀態轉移
- AI governance: AI 頻道白名單與 AI Agent 頻道啟用是兩套設定；討論串會繼承父頻道 Agent 配置

## 2. Change Hotspots

- Critical modules:
  - `src/main/java/ltdjms/discord/panel/commands/`：大量 Discord 互動狀態與 UI 字串集中在此
  - `src/main/java/ltdjms/discord/shop/services/`：付款、履約、通知與 callback 冪等邏輯集中於此
  - `src/main/java/ltdjms/discord/dispatch/`：訂單狀態流轉與售後規則
  - `src/main/java/ltdjms/discord/shared/`：設定載入、事件管道、快取、DI
  - `src/main/java/ltdjms/discord/aichat/` 與 `src/main/java/ltdjms/discord/aiagent/`：模型設定、工具鏈、記憶與頻道授權
- External dependency touchpoints:
  - Discord API / JDA
  - PostgreSQL + Flyway
  - Redis / Lettuce
  - ECPay callback / payment API
  - OpenAI-compatible AI provider
  - 商品後端 fulfillment webhook
- Data integrity or migration risks:
  - 修改商品、法幣訂單、派單狀態欄位時需同步 Flyway migration、repository 映射與測試
  - callback / 履約流程存在冪等欄位與 claim 流程，調整時要先理解既有狀態鎖定
- Concurrency / idempotency concerns:
  - `FiatPaymentCallbackService` 透過 repository claim / mark 方法避免重複通知與重複履約
  - `DomainEventPublisher` 雖支援多 listener，但仍是同步串行分發
  - 面板 ephemeral session 與多步驟互動不可隨意改動 custom ID / modal ID

## 3. Testing Expectations

- Required test layers:
  - 單元測試：service、domain、handler 分支與錯誤路徑
  - 整合測試：repository、Dagger wiring、重要事件管線與資料庫鏈路
  - Property-based：僅在存在明確不變式的業務邏輯上新增；repo 目前已有 `gametoken/property` 範例
- Mock/fake expectations:
  - ECPay、AI provider、Discord 互動、外部 fulfillment webhook 應以 mock / fake 隔離
  - callback、通知與 UI 狀態切換要驗證 side effect，不只驗證「沒有丟例外」
- High-value regression areas:
  - `/admin-panel` 的多步互動流程與 embed 長度限制
  - 商品購買後的退款、履約通知與法幣 callback 冪等
  - 事件 listener wiring 與快取失效
  - AI 頻道限制、Agent 頻道啟用與討論串父頻道繼承邏輯

## 4. Debugging Entry Points

- Logs:
  - 本機：`logs/app.log`, `logs/warn.log`, `logs/error.log`
  - Docker：`make logs`
- Local reproduction commands:
  - `make build`
  - `make test`
  - `make test-integration`
  - `make verify`
  - `make start-dev`
  - `java -jar target/ltdjms-*.jar`
- Dashboards or observability tools:
  - `None in repository evidence`
- Common failure signals:
  - `Discord bot token not configured`
  - `AI service API key not configured`
  - `公開 ECPay callback 綁定必須設定 shared secret`
  - Redis 初始化失敗或連線錯誤
  - slash command sync 失敗或 guild 權限不足

## 5. Documentation Maintenance Notes

- Specs reviewed:
  - `2026-03-04 ecpay-payment-callback-fulfillment`
  - `2026-03-05 admin-panel-settings-embed-workflow`
  - `2026-03-10 unified-domain-event-pipeline`
- Existing docs updated:
  - `README.md`
  - `docs/README.md`
  - `docs/getting-started.md`
  - `docs/configuration.md`
  - `docs/architecture.md`
  - `docs/features.md`
  - `docs/developer-guide.md`
- Major conflicts resolved:
  - `Makefile` help 與部分舊文件提到 `make run`，但目前沒有對應 target；已改為使用 `java -jar`
  - 舊文件多處提到 `AI_SERVICE_MAX_TOKENS`，但現行 `EnvironmentConfig` 未支援該變數；已以程式碼現況為準
  - 舊文件描述 Redis 可自動降級為 NoOp，但現行 `CacheModule` 直接建立 `RedisCacheService`；已改為記錄真實 wiring 行為
  - 資料庫設定文件改為明確說明 `DB_URL` 與 `DATABASE_*` 的優先序與舊別名 `DATABASE_USER` / `DATABASE_PASSWORD`
- Remaining unknowns:
  - `application.conf` 仍保留較舊的鍵名結構，與 `application.properties` / `EnvironmentConfig` 並存
  - repo 沒有完整的生產部署 / 回滾 runbook
