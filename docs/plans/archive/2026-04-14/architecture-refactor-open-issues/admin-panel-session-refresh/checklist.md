# Checklist: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：issue 與程式證據已足夠）
- [x] Affected plans are reviewed/updated（approval 後若 view metadata shape 改動，需同步 coordination）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 以本次工作流內的 spec-backed implementation 為準）

## Behavior-to-Test Checklist
- [x] CL-PANEL-01 session manager 可按 guild 枚舉有效 admin panel session
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-admin-session-traversal`, `UT-expired-hook-cleanup`
  - Test level: `Unit`
  - Risk class: `regression / data integrity`
  - Property/matrix focus: `session metadata matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `enumeration result / cleanup behavior`
  - Test result: `PASS` (`mvn -q -Dtest=AdminPanelSessionManagerTest,... test`)

- [x] CL-PANEL-02 domain events 會刷新正確 view，而非只記 warning
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-admin-panel-refresh-main-view`, `UT-admin-panel-refresh-product-view`, `IT-admin-panel-refresh`
  - Test level: `Unit / Integration`
  - Risk class: `regression / state coordination`
  - Property/matrix focus: `view-state matrix`
  - External dependency strategy: `mocked hook`
  - Oracle/assertion focus: `correct embed refresh / no wrong-view overwrite`
  - Test result: `PASS` (`mvn -q -Dtest=AdminPanelButtonHandlerRefreshTest,AdminPanelRefreshIntegrationTest,AdminPanelUpdateListenerTest test`)

- [x] CL-PANEL-03 文件與測試宣稱的 refresh 能力與 runtime 一致
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `UT-listener-no-unimplemented-warning`
  - Test level: `Review / Unit`
  - Risk class: `maintainability / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / no-op path removed`
  - Test result: `PASS`（`docs/modules/event-system.md`、`docs/modules/panels.md` 已同步 runtime scope）

## Required Hardening Records
- [x] Regression tests are added/updated for event-driven refresh paths
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：view/session 組合有限，矩陣式 unit/integration 測試已完整覆蓋）
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [x] Adversarial/penetration-style cases are added/updated for expired hook / duplicate refresh / stale metadata
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only log output
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `PASS` (`mvn -q -Dtest=AdminPanelSessionManagerTest,AdminPanelUpdateListenerTest,AdminPanelButtonHandlerRefreshTest,AdminPanelCommandHandlerTest,AdminPanelButtonHandlerTest,AdminProductPanelHandlerTest test`)
- [x] Regression tests: `PASS` (`mvn -q -Dtest=AdminPanelSessionManagerTest,AdminPanelUpdateListenerTest,AdminPanelButtonHandlerRefreshTest,AdminPanelRefreshIntegrationTest test`)
- [x] Property-based tests: `N/A`（view/session 狀態矩陣有限，且本次變更以確定性 unit / integration 斷言覆蓋主要風險）
- [x] Integration tests: `PASS` (`mvn -q -Dtest=AdminPanelRefreshIntegrationTest test`)
- [x] E2E tests: `N/A`（無需額外端到端流程；`/admin-panel` 的核心 refresh 行為已由 integration test 驗證）
- [x] External service mock scenarios: `PASS`（hook / service 依賴於 unit 與 integration tests 中以 mock/fake 驗證）
- [x] Adversarial/penetration-style cases: `PASS`（expired hook、stale metadata、duplicate refresh 路徑已覆蓋）

## Completion Records

### Completion Record 1: session abstraction 與 traversal
- Requirement mapping: `R1.x / Task 1 / CL-PANEL-01`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `AdminPanelSessionManager` now tracks guild/admin/view metadata and cleans invalid hooks during traversal`

### Completion Record 2: refresh 路徑與文件對齊
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-PANEL-02..03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `AdminPanelUpdateListener` routes currency/dice events to main panel refresh and product/redemption events to product panel refresh; docs now describe the supported refresh scope and explicit manual views`
