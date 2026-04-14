# Checklist: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與上游/本地證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若 terminal status 命名改動，需同步 snapshot spec 與 coordination）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-EXPIRY-01 未付款訂單會在期限後轉成 terminal state
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-expire-transition`, `IT-order-expires-after-deadline`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / regression`
  - Property/matrix focus: `time-state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / no lingering pending`
  - Test result: `NOT RUN`

- [ ] CL-EXPIRY-02 reconciliation 只處理仍在有效期內的 pending order
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-reconciliation-selection`, `IT-reconciliation-excludes-expired`, `UT-paid-vs-expired-race`
  - Test level: `Unit / Integration`
  - Risk class: `concurrency / external failure / invalid transition`
  - Property/matrix focus: `external state matrix / race invariant`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `single final truth / no duplicate transition`
  - Test result: `NOT RUN`

- [ ] CL-EXPIRY-03 文案與查詢會顯示 expired/cancelled 語意
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `UT-direct-message-expiry-wording`, `DOC-review`
  - Test level: `Unit / Review`
  - Risk class: `regression / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / buyer-facing copy`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for unpaid-expiry and near-deadline paid cases
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：時間/狀態矩陣有限，離散測試更直觀）
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for race、重試與缺少 expireAt 的歷史資料
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only query method 被呼叫
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: expiry transition 與 reconciliation
- Requirement mapping: `R1.x-R2.x / CL-EXPIRY-01..02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-order-expires-after-deadline`, `IT-reconciliation-excludes-expired`
- Reason: 風險集中在 DB state transition 與 scheduler/query interaction

### Decision Record 2: buyer-facing wording
- Requirement mapping: `R3.x / CL-EXPIRY-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `UT-direct-message-expiry-wording`
- Reason: 文案驗證可透過單元測試與文件檢視完成

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: terminal state 與 expiry transition
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-EXPIRY-01..02`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: 文案、查詢與文件對齊
- Requirement mapping: `R3.x / Task 3 / CL-EXPIRY-03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
