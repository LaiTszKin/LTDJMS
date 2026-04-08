# Checklist: Issue 73 Agent 工具輸出記憶隔離

- Date: 2026-04-09
- Feature: Issue 73 Agent 工具輸出記憶隔離

## Clarification & Approval Gate
- [ ] User clarification responses are recorded — `N/A`（目前需求已足夠明確）
- [ ] Affected plans are reviewed/updated — `N/A`（尚未進入 clarification update）
- [ ] Explicit user approval on updated specs is obtained（待本批 specs 審核）

## Behavior-to-Test Checklist

- [ ] CL-73-01 工具結果不再被保存成 raw `AiMessage`
  - Requirement mapping: `R1.1-R1.2`
  - Actual test case IDs: `UT-73-01`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證 `InMemoryToolCallHistory` 的輸出已改為 safe summary 或空

- [ ] CL-73-02 `searchMessages` 類型高風險結果不會在後續 memory rehydration 時再次出現
  - Requirement mapping: `R2.3`, `R3.1`
  - Actual test case IDs: `UT-73-02`, `UT-73-03`
  - Test level: `Unit`
  - Risk class: `adversarial abuse`
  - Property/matrix focus: `adversarial case`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `permission denial`
  - Test result: `NOT RUN`
  - Notes (optional): 需覆蓋 snippet、jump URL、作者資訊被紅線化或省略

- [ ] CL-73-03 thread history 與 user-scoped isolation 保持相容
  - Requirement mapping: `R3.2-R3.3`
  - Actual test case IDs: `UT-73-04`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證合法 thread history 仍載入，legacy API 與不同 user 隔離不破壞

## Required Hardening Records
- [ ] Regression tests are added/updated for bug-prone or high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures)

## E2E / Integration Decision Records

### Decision Record 1: tool-result memory isolation
- Requirement mapping: `R1.x-R3.x / CL-73-01~03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `InMemoryToolCallHistoryTest`, `SimplifiedChatMemoryProviderTest` 擴充後案例
- Reason: 風險集中於 memory 組裝邏輯，單元測試可比整體 agent E2E 更精準地證明 raw result 是否被重新注入

### Decision Record 2: 當回合工具能力相容
- Requirement mapping: `R3.3 / CL-73-03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-73-01`（如需要）
- Reason: 若單元測試後仍擔心 agent 當回合工具能力受影響，可加一個近整合 case；不需完整 Discord E2E

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: 規格與設計
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-73-01~03`
- Completion status: `completed`
- Remaining applicable items: `Implementation and verification`
- Notes: 本 spec 明確把審計資料與模型上下文分流，避免「能調試」與「可被模型再次讀取」被誤當成同一件事

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `deferred`
- Remaining applicable items: `All implementation and test tasks`
- Notes: 本文件目前僅為待審批的 planning set
