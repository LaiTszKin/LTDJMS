# Checklist: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：目前 issue 與 repo 證據已足以定義需求）
- [ ] Affected plans are reviewed/updated（若 approval 後調整 decision matrix，需同步更新五份文檔）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-GATE-01 Agent-only channel mention 會走 Agent 路徑
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-routing-agent-only`, `UT-listener-agent-bypass-allowlist`
  - Test level: `Unit`
  - Risk class: `authorization / regression`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `decision result / side effects / no silent drop`
  - Test result: `NOT RUN`

- [ ] CL-GATE-02 Non-agent channel 仍遵守 AI allowlist
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-routing-ai-only`, `UT-listener-no-allowlist-deny`
  - Test level: `Unit`
  - Risk class: `authorization / regression`
  - Property/matrix focus: `decision matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `permission denial / unchanged fallback behavior`
  - Test result: `NOT RUN`

- [ ] CL-GATE-03 文件、panel 與測試都以相同 routing matrix 表達行為
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `UT-agent-conclusion-updated`
  - Test level: `Unit / Review`
  - Risk class: `regression / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / test parity`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for mention gating high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：decision matrix 有限且離散，表格化單元測試比 PBT 更可讀）
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason（`N/A`：無外部服務鏈）
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only「does not throw」
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: mention routing matrix
- Requirement mapping: `R1.x-R2.x / CL-GATE-01..02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-mention-routing-matrix`
- Reason: 風險集中在 listener 與 service wiring；不需要真 Discord E2E 即可驗證

### Decision Record 2: panel / docs parity
- Requirement mapping: `R3.x / CL-GATE-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `doc review + targeted unit tests`
- Reason: 風險在語意一致性，不需要額外 E2E

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `N/A`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: mention routing 重構
- Requirement mapping: `R1.x-R2.x / Task 1 / CL-GATE-01..02`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待使用者批准後實作`

### Completion Record 2: 文件與測試對齊
- Requirement mapping: `R3.x / Task 2-3 / CL-GATE-03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未進入 implementation phase`
