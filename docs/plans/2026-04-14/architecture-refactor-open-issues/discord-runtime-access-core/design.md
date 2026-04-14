# Design: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core
- Change Name: discord-runtime-access-core

## Design Goal
用正式注入的 runtime gateway 取代 `JDAProvider` 的 owner 角色，讓 bootstrap lifecycle 與業務模組依賴邊界清楚分離。

## Change Summary
- Requested change: 修正 `JDAProvider` 成為跨模組 process-global integration boundary 的問題
- Existing baseline: `DiscordCurrencyBot` 啟動後把 live JDA 寫進 static `JDAProvider`，多個模組直接 `getJda()`
- Proposed design delta: 在 `shared/di` 與 `discord` 模組中建立 `DiscordRuntimeGateway`（名稱可調整），由 bootstrap 發布，其他模組透過注入依賴它

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `shared/di`, `discord`, `currency/bot`, docs/tests
- External contracts involved: `JDA runtime / builder lifecycle`
- Coordination reference: `../coordination.md`

## Current Architecture
- JDA 在啟動完成後才可取得，但 repo 目前以 static singleton 方式暴露給各模組
- 這使文件宣稱的 `discord` abstraction layer 失去實質 owner 地位
- 測試若要驗證這些模組，也必須先 mutate 同一個 global singleton

## Proposed Architecture
- 引入一個 singleton 但非 static 的 `DiscordRuntimeGateway` / `DiscordRuntimeHandle`
- bootstrap 在 JDA ready 後將 runtime 注入/發布到此 gateway
- 其他模組只依賴 gateway 介面或更窄的 adapter，不直接依賴 `JDA` 或 `JDAProvider`
- `JDAProvider` 若暫時保留，只作為遷移橋接，並被明示為 legacy shim

## Component Changes

### Component 1: `DiscordRuntimeGateway`
- Responsibility: 封裝 live Discord runtime access 與 ready/not-ready 狀態
- Inputs: bootstrap 發布的 JDA instance
- Outputs: 最小化的 guild/user/channel lookup 能力與 bot identity access
- Dependencies: JDA runtime
- Invariants: not-ready 狀態可測且明確；不向全 repo 無限制暴露原始 JDA

### Component 2: `DiscordCurrencyBot` / bootstrap wiring
- Responsibility: 在啟動完成後發布 runtime handle
- Inputs: built JDA instance
- Outputs: gateway ready state
- Dependencies: Dagger component、gateway abstraction
- Invariants: 發布動作僅在 ready lifecycle 合法時發生

### Component 3: compatibility bridge
- Responsibility: 為尚未遷移 call sites 暫時提供過渡讀取
- Inputs: gateway 或 legacy static state
- Outputs: transitional access path
- Dependencies: `JDAProvider` 或等價 shim
- Invariants: 不可成為新的長期 canonical owner

## Sequence / Control Flow
1. Dagger 建立 core component 與 runtime gateway
2. bootstrap 建立 JDA，等待可用
3. bootstrap 將 ready runtime 發布到 gateway
4. 已遷移模組透過 gateway 存取 Discord runtime；未遷移模組暫時走 bridge
5. call site migration 完成後移除 bridge

## Data / State Impact
- Created or updated data: 無持久化資料；新增 runtime gateway 與測試 fixture
- Consistency rules: 任何新模組不得再新增 direct static runtime access
- Migration / rollout needs: 允許 bridge 存在一個短期 compatibility window

## Risk and Tradeoffs
- Key risks: gateway 介面設計過寬、bridge 長期殘留、bootstrap 與 DI graph 的初始化順序不清
- Rejected alternatives:
  - 維持 `JDAProvider`：無法恢復 abstraction boundary
  - 直接把 `JDA` 注入所有模組：只把 global 問題換成更廣的 framework coupling
- Operational constraints: not-ready path 需要清楚 exception / result semantics，避免執行期出現模糊 null

## Validation Plan
- Tests: `UT` ready/not-ready semantics、bridge guard、`IT` bootstrap publishes runtime once
- Contract checks: 驗證設計尊重 JDA bootstrap lifecycle
- Rollback / fallback: 若 gateway 方案需要回退，可暫時保留 bridge，但不得再次接受新的 direct static dependency

## Open Questions
None
