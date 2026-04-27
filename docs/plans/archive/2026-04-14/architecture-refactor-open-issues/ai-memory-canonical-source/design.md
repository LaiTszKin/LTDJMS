# Design: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source
- Change Name: ai-memory-canonical-source

## Design Goal
把 AI Agent 對話記憶收斂成一條正式 runtime path，並把其他殘留 artifacts 降級為明示的 audit/legacy/deprecation 資產或移除。

## Change Summary
- Requested change: 修正 conversation persistence schema、deprecated providers、文件與 runtime memory wiring 彼此脫節的問題
- Existing baseline: `AIAgentModule` 實際接線到 `SimplifiedChatMemoryProvider`；舊 PostgreSQL/Redis path 雖仍保留，但已明確降級為 legacy / compatibility / audit 資產
- Proposed design delta: 鎖定 `SimplifiedChatMemoryProvider` 為單一 canonical runtime owner，並將其餘 artifacts 一律標示為 non-runtime 或 legacy

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `aiagent/services`, `aiagent/persistence`, `shared/di`, migration/docs/tests
- External contracts involved: `LangChain4j ChatMemory / ChatMemoryProvider`
- Coordination reference: `../coordination.md`

## Current Architecture
- runtime 使用 `SimplifiedChatMemoryProvider`，從 Discord thread history + in-memory tool call history 組上下文
- 舊 `PersistentChatMemoryProvider`、`JdbcConversationMessageRepository`、`RedisPostgresChatMemoryStore` 仍存在，但僅作 legacy / compatibility / audit 用途，不在正式 runtime path 上
- `V012__agent_conversation_persistence.sql` 與文件已更新為讓維護者一眼看出 PostgreSQL conversation tables 不屬於 runtime canonical memory

## Proposed Architecture
- 明確把「runtime conversation memory」與「非 runtime 診斷資料」分開
- 以目前實際接線的 `SimplifiedChatMemoryProvider` 作為 canonical runtime owner，並把舊 persistence path 保留為 audit/legacy/compatibility 資產
- 若未來需要持久化工具調用或診斷資訊，應建立明確的 audit abstraction，而不是重用舊 conversation memory 名稱

## Component Changes

### Component 1: `AIAgentModule`
- Responsibility: 只接線單一 canonical `ChatMemoryProvider`
- Inputs: thread history provider、tool history provider、可選 audit services
- Outputs: runtime `ChatMemoryProvider`
- Dependencies: LangChain4j memory abstraction
- Invariants: runtime memory owner 唯一且文件化

### Component 2: `SimplifiedChatMemoryProvider` / canonical provider
- Responsibility: 為 runtime 會話提供實際上下文
- Inputs: conversation id、Discord thread history、in-memory tool call history
- Outputs: `ChatMemory`
- Dependencies: thread history provider、tool history provider
- Invariants: restart semantics、thread-only semantics 必須明示；不得隱性宣稱 persistent restore

### Component 3: legacy persistence artifacts
- Responsibility: 保留為明示的 audit / compatibility / migration 資產，不再承擔 runtime memory
- Inputs: 既有 migration / repository / docs
- Outputs: 清楚的 deprecation / migration 結果
- Dependencies: schema、docs、tests
- Invariants: 不可再與 runtime owner 混名

## Sequence / Control Flow
1. 選定 canonical runtime owner
2. 讓 `AIAgentModule`、文件、測試全部只指向這條 runtime path
3. 對舊 persistence artifacts 決定 drop / rename / repurpose 策略，並保留必要 compat 介面
4. 更新 migration、命名與 docs，使維護者能直接辨識 runtime 與 non-runtime 資產

## Data / State Impact
- Created or updated data: conversation tables 以 legacy/audit 語境保留，文件與 deprecation 標記同步更新
- Consistency rules: 只能有一條 runtime memory canonical owner；其餘資料若保留，必須有不同名稱與用途
- Migration / rollout needs: 目前採保留兼容方向；若日後 drop 舊 tables，需另行處理歷史資料保留策略

## Risk and Tradeoffs
- Key risks: 直接刪表可能影響潛在診斷用途、保留舊表但未改名又繼續誤導維護者
- Rejected alternatives:
  - 保持現狀：平行架構持續誤導
  - 把未實作 persistence 直接宣告為 canonical：與現況不符，且需補齊更多 runtime 複雜度
- Operational constraints: 若選擇保留非 runtime audit data，必須有清楚 retention 與讀寫責任

## Validation Plan
- Tests: `UT` canonical wiring、restart semantics、deprecated path guard；migration comments / docs backfill 驗證
- Contract checks: 驗證選定 provider 的 LangChain4j 抽象語意與命名一致
- Rollback / fallback: 若 drop/rename 方案過大，可先加清楚 deprecation naming 與 docs，再分階段清理 schema

## Open Questions
None
