# Checklist: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與程式證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若 view metadata shape 改動，需同步 coordination）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-PANEL-01 session manager 可按 guild 枚舉有效 admin panel session
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-admin-session-traversal`, `UT-expired-hook-cleanup`
  - Test level: `Unit`
  - Risk class: `regression / data integrity`
  - Property/matrix focus: `session metadata matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `enumeration result / cleanup behavior`
  - Test result: `NOT RUN`

- [ ] CL-PANEL-02 domain events 會刷新正確 view，而非只記 warning
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-admin-panel-refresh-main-view`, `UT-admin-panel-refresh-product-view`
  - Test level: `Unit`
  - Risk class: `regression / state coordination`
  - Property/matrix focus: `view-state matrix`
  - External dependency strategy: `mocked hook`
  - Oracle/assertion focus: `correct embed refresh / no wrong-view overwrite`
  - Test result: `NOT RUN`

- [ ] CL-PANEL-03 文件與測試宣稱的 refresh 能力與 runtime 一致
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `UT-listener-no-unimplemented-warning`
  - Test level: `Review / Unit`
  - Risk class: `maintainability / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / no-op path removed`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for event-driven refresh paths
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：view/session 組合有限，可用矩陣測試）
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [ ] Adversarial/penetration-style cases are added/updated for expired hook / duplicate refresh / stale metadata
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only log output
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: guild-wide panel refresh
- Requirement mapping: `R1.x-R2.x / CL-PANEL-01..02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-admin-panel-refresh`
- Reason: 需要驗證 listener + session manager + hook 編輯的跨元件配合

### Decision Record 2: 文件與能力對齊
- Requirement mapping: `R3.x / CL-PANEL-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `doc review + targeted unit tests`
- Reason: 不需要另建 E2E

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: session abstraction 與 traversal
- Requirement mapping: `R1.x / Task 1 / CL-PANEL-01`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: refresh 路徑與文件對齊
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-PANEL-02..03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
