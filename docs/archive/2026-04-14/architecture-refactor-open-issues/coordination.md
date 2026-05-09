# Coordination: architecture-refactor-open-issues

- Date: 2026-04-14
- Batch: architecture-refactor-open-issues
- Goal: 針對目前 7 張仍開啟的架構 issue，拆成 8 份可獨立審批與實作的重構規格，消除執行期隱性耦合、補齊缺失的狀態模型，並讓文件、資料模型與實際執行路徑重新一致。

## Shared Direction
- `fiat_order` 仍是法幣付款真相的唯一來源；新增的到期、履約快照與護航交接資訊都必須圍繞這個真相模型展開。
- `dispatch` 必須成為護航工作流的唯一 durable aggregate；管理員 DM 只能是從 dispatch 工作項衍生的通知，不可再作為唯一交接機制。
- AI Chat 白名單與 AI Agent 啟用清單維持兩套設定域；mention 入口應根據頻道模式決定走哪一條鏈路，不可再硬耦合為單一 AND 條件。
- `discord` / `shared` 的注入邊界必須重新成為 Discord runtime access 的唯一正式入口；任何 compatibility bridge 都只能是暫時過渡層。
- AI Agent 對話記憶必須明確指定單一 canonical owner；若保留非執行期資料表，必須改為明示的 audit/diagnostic 用途，不可再冒充 runtime memory。

## Batch Scope
- Included spec sets:
  - `ai-agent-channel-gating`
  - `escort-order-handoff`
  - `fiat-order-fulfillment-snapshot`
  - `fiat-order-expiry-lifecycle`
  - `admin-panel-session-refresh`
  - `ai-memory-canonical-source`
  - `discord-runtime-access-core`
  - `discord-runtime-access-callsite-migration`
- Shared outcome: 讓 channel gating、付款後履約、護航交接、面板更新、AI 記憶與 Discord runtime access 都回到可推理、可測試、可觀測的單一責任邊界。
- Out of scope:
  - 直接實作本批次程式碼
  - 重做 slash command / panel 的產品體驗或文案策略
  - 引入新的外部 message queue、workflow engine 或完整事件溯源系統
- Independence rule: 每份 spec 都必須能以 additive schema、compatibility adapter 或單點責任內聚的方式獨立落地；任何規格都不可要求另一份 spec 先 merge 才能維持系統正確性。

## Shared Context
- Current baseline: 系統目前存在多個「文件宣告的邊界」與「實際執行路徑」不一致的問題，集中在 mention gating、Discord runtime access、AI 記憶、法幣訂單生命週期與護航交接。
- Shared constraints:
  - 不可破壞既有 slash command、custom ID、modal ID 與 session 語意。
  - 不可讓付款真相、護航工作項或面板更新重新落回人工口頭流程。
  - 不可引入要求停機的大規模一次性切換；需偏向漸進式 refactor。
  - 事件系統仍為同步分發；任何 listener 變更不得把長耗時副作用再塞回主流程。
- Shared invariants:
  - `fiat_order.order_number` / `MerchantTradeNo` 語意不變。
  - `dispatch` 的狀態流轉仍由 aggregate 約束，而非由通知文案暗示。
  - mention 入口仍只處理 guild 訊息，不回應 bot 訊息與 DM。
  - JDA 實例仍在 bot 啟動完成後才可用，但其可用性不應再透過 process-global static 狀態向業務模組外洩。

## Shared Preparation

### Shared Fields / Contracts
- Shared fields to introduce or reuse:
  - `fiat_order`：到期時間、終止狀態、履約快照欄位
  - `escort_dispatch_order`：來源訂單、來源商品、escort option、價格快照等來源脈絡欄位
  - `DiscordRuntime` / 等價 gateway：JDA 存取的注入式邊界
  - 可枚舉的 admin panel session metadata / guild index
- Canonical source of truth:
  - 付款與到期：`shop`
  - 護航工作流：`dispatch`
  - Discord runtime access：`discord` / `shared`
  - AI conversation memory ownership：`aiagent`
- Required preparation before implementation:
  - 共享 schema 變更採 additive-first，避免不同 spec 對同一 migration 產生互斥要求
  - `JDAProvider` 在整批完成前可暫時保留為 compatibility bridge，但新程式不得再擴散新的直接依賴
  - 若保留舊資料表或舊通知路徑，必須在文件中標明 transitional-only

### Replacement / Legacy Direction
- Legacy behavior being replaced:
  - Agent mention 被 AI 白名單前置攔截
  - 護航自動開單停在 admin DM
  - 法幣訂單只靠 live product 決定履約與到期狀態
  - admin panel guild-wide refresh 為 no-op
  - AI persistent memory 與 runtime path 分裂
  - JDAProvider 成為實質 runtime 邊界
- Required implementation direction: replace in place，必要時使用短期 adapter / fallback bridge，但不得新增第二條長期 canonical path。
- Compatibility window: 有。`JDAProvider`、舊 admin DM、舊 conversation persistence artifacts 可作為短期過渡，但必須在各 spec 中明列保留條件與拆除時機。
- Cleanup required after cutover:
  - 刪除/降級舊白名單耦合判斷
  - 將 admin DM 降為 dispatch 派生通知
  - 移除或重新命名 orphaned conversation persistence artifacts
  - 收斂 `JDAProvider` 只剩 bootstrap bridge 或完全移除

## Spec Ownership
- `ai-agent-channel-gating/`
  - owns：`aichat` mention entry decision、`aiagent` 啟用判斷接線、相關 panel/doc 對齊
  - may touch：`src/main/java/ltdjms/discord/aichat/**`、`src/main/java/ltdjms/discord/aiagent/**`、`src/main/java/ltdjms/discord/panel/**`、相關測試與文件
- `escort-order-handoff/`
  - owns：商品/付款完成後進入 `dispatch` aggregate 的 durable handoff 邊界
  - may touch：`product/`、`shop/`、`dispatch/`、對應 migration 與測試
- `fiat-order-fulfillment-snapshot/`
  - owns：`fiat_order` 的履約契約快照與 post-payment replay 規則
  - may touch：`shop/`、`product/`、對應 migration 與測試
- `fiat-order-expiry-lifecycle/`
  - owns：`fiat_order` 未付款逾期的 terminal state、掃描與文件承諾對齊
  - may touch：`shop/`、對應 migration 與測試、必要文件
- `admin-panel-session-refresh/`
  - owns：admin panel session 可枚舉性與 guild-wide refresh 抽象
  - may touch：`panel/`、`discord/`、相關事件測試
- `ai-memory-canonical-source/`
  - owns：AI Agent conversation memory 的 canonical owner、文件、deprecated path 清理策略
  - may touch：`aiagent/`、`shared/di`、對應 migration/docs/tests
- `discord-runtime-access-core/`
  - owns：注入式 Discord runtime gateway、bootstrap wiring、compatibility bridge
  - may touch：`shared/di/`、`discord/`、`currency/bot/`
- `discord-runtime-access-callsite-migration/`
  - owns：`aiagent`、`aichat`、`shop` 內的 JDAProvider call site 遷移與測試隔離
  - may touch：`src/main/java/ltdjms/discord/aiagent/**`、`src/main/java/ltdjms/discord/aichat/**`、`src/main/java/ltdjms/discord/shop/**`、相關測試

## Conflict Boundaries
- Shared files requiring coordination:
  - `src/main/java/ltdjms/discord/shop/domain/FiatOrder.java`
  - `src/main/java/ltdjms/discord/shop/persistence/JdbcFiatOrderRepository.java`
  - `src/main/resources/db/migration/*fiat*`
  - `src/main/java/ltdjms/discord/shared/di/AIAgentModule.java`
  - `src/main/java/ltdjms/discord/shared/di/JDAProvider.java`
- File ownership / edit guardrails:
  - `FiatOrder` / repository / migration 的到期欄位由 `fiat-order-expiry-lifecycle` 擁有
  - `FiatOrder` / repository / migration 的履約快照欄位由 `fiat-order-fulfillment-snapshot` 擁有
  - `AIAgentModule` 的 memory wiring 由 `ai-memory-canonical-source` 擁有；runtime gateway wiring 由 `discord-runtime-access-core` 擁有
  - `shop` 通知與 worker 的 dispatch handoff 由 `escort-order-handoff` 擁有
- Shared API / schema freeze:
  - `DiscordRuntime` 介面命名與核心方法由 `discord-runtime-access-core` 決定；其他 spec 只能 additive-only 使用
  - `fiat_order` 新欄位命名以 `shop` 為 canonical owner；其他 spec 不得各自創建語意重疊欄位
- Compatibility shim retention rules:
  - `JDAProvider` 只在所有 owned call sites 遷移完成後才可刪除
  - admin DM 在 `dispatch` durable handoff 上線前可保留，但上線後只能作為 dispatch 衍生通知
- Merge order / landing order: 建議先落 `discord-runtime-access-core`、`fiat-order-fulfillment-snapshot`、`ai-memory-canonical-source`，再落 call site / lifecycle / handoff specs；但此順序僅為操作便利，不是功能前提。
- Worktree notes: 若平行實作，分支名稱應包含 spec 名稱；變更共享檔案前先比對 coordination 規則。

## Integration Checkpoints
- Combined behaviors to verify after merge:
  - Agent-only channel 可在未加入 AI 白名單時正確進入 Agent 工具鏈
  - 已付款護航商品能落成可追蹤的 `EscortDispatchOrder`
  - `fiat_order` 在未付款逾期與已付款履約兩種路徑都具有完整且穩定的狀態語義
  - admin panel 在 guild-wide 事件後可即時刷新而非只記 warning
  - AI memory 文件、runtime 與資料表語義一致
  - `aiagent` / `aichat` / `shop` 測試不再依賴 process-global JDA mutable state
- Required final test scope: 單元測試、整合測試、重播/冪等矩陣、必要的 migration 驗證。
- Rollback notes: additive schema 可先保留；若單一 spec 回滾，不得留下兩套 canonical owner 並存的永久狀態。

## Open Questions
None
