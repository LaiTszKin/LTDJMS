# Design: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration
- Change Name: discord-runtime-access-callsite-migration

## Design Goal
把 `aiagent`、`aichat`、`shop` 從 static global JDA 依賴遷移到可測、可替換的窄 adapter / gateway，讓這些模組重新符合 repo 的 abstraction 邊界。

## Change Summary
- Requested change: 移除指定模組對 `JDAProvider` 的直接依賴
- Existing baseline: 多個 services/listeners 直接 `JDAProvider.getJda()`，測試也以 `setJda()/clear()` 操作同一個全域狀態
- Proposed design delta: 所有 owned call sites 改為 constructor injection 的 gateway/adapter；測試改用 fake runtime dependency；新增 guardrail 防止回流

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `aiagent`, `aichat`, `shop`, 相關測試與文件
- External contracts involved: `JDA runtime object model`
- Coordination reference: `../coordination.md`

## Current Architecture
- `aiagent`：頻道配置、thread history、tool execution listeners 等直接抓 static JDA
- `shop`：buyer/admin DM 通知服務直接依賴 static JDA
- `aichat`：聊天服務與可能的 bot identity 取值直接依賴 static JDA
- 測試套件為了讓這些物件可跑，必須共享與清理同一個 global singleton

## Proposed Architecture
- 依模組需求拆成窄 adapter，例如：guild/member lookup、thread history、direct message sender、bot identity provider
- 各 service/listener 只注入其需要的最小 adapter，不直接拿 runtime gateway 全表面積
- 測試以 fake adapter / fake runtime gateway 取代 static setup，並用 guardrail 防止 direct static dependency 回流

## Component Changes

### Component 1: `aiagent` adapters
- Responsibility: 封裝 thread history、channel config 所需的 Discord runtime operations
- Inputs: thread/channel/user identifiers
- Outputs: domain-friendly lookup results / errors
- Dependencies: core runtime gateway
- Invariants: tool execution 與 thread history 不直接綁定 static global state

### Component 2: `shop` notification adapters
- Responsibility: 封裝 guild lookup、admin/member traversal、buyer/admin DM 發送
- Inputs: guildId、userId、message payload
- Outputs: 發送結果或可觀測失敗
- Dependencies: core runtime gateway
- Invariants: DM failure 不改變業務真相；service 不直接抓 JDA

### Component 3: guardrail / test helpers
- Responsibility: 防止 direct static call site 回流，並提供 module-local fake fixtures
- Inputs: search rules、fake gateway、test utilities
- Outputs: CI / test failure、可重用測試依賴
- Dependencies: test infrastructure
- Invariants: bootstrap/core 合法 bridge 不被誤判為一般模組 call site

## Sequence / Control Flow
1. core spec 提供 runtime gateway / bridge
2. `aiagent`、`aichat`、`shop` 逐一改為 constructor injection 的窄 adapter
3. 測試改用 fake adapter，移除 `setJda()/clear()` 依賴
4. 新增 guardrail，防止 direct `JDAProvider.getJda()` 再次出現在 owned modules
5. 文件記錄新的 migration pattern 與剩餘 bridge 範圍

## Data / State Impact
- Created or updated data: 無持久化資料；更新 constructor wiring、test helpers、guardrail
- Consistency rules: owned modules 一律使用 injected adapter；不得一半走 adapter、一半直接 static
- Migration / rollout needs: 可分模組漸進遷移，但每個模組內部要維持單一 canonical access path

## Risk and Tradeoffs
- Key risks: adapter 過細導致樣板碼增加、過寬導致 abstraction 失效、舊測試 fixture 未完全清理
- Rejected alternatives:
  - 直接注入整個 JDA：耦合仍然過重
  - 只改測試不改正式碼：無法解決 runtime boundary 問題
- Operational constraints: guardrail 需精準，不可阻擋 bootstrap/core 合法 bridge 檔案

## Validation Plan
- Tests: `UT` fake adapter matrix、`IT` notification/thread-history path、搜尋 guardrail
- Contract checks: 驗證 adapter 封裝的 JDA surface 與業務需求相稱
- Rollback / fallback: 單模組若需回退，可暫時走 bridge，但需保留 guardrail 與遷移紀錄，不可重新散播 direct static call site

## Open Questions
None
