# Checklist: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與現況證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若調整 snapshot shape，需同步 coordination 與 escort handoff spec）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-SNAPSHOT-01 建單時會寫入完整履約快照
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-fiat-order-create-snapshot`, `IT-persist-contract-fields`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / regression`
  - Property/matrix focus: `field matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `persisted snapshot / validation failure`
  - Test result: `NOT RUN`

- [ ] CL-SNAPSHOT-02 paid worker 只 replay order snapshot
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-worker-snapshot-only`, `IT-product-changed-after-order`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / replay-idempotency`
  - Property/matrix focus: `state replay matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `no live-product dependency / no duplicate side effect`
  - Test result: `NOT RUN`

- [ ] CL-SNAPSHOT-03 文件與命名顯示 order 是履約契約 owner
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `schema review`
  - Test level: `Review`
  - Risk class: `regression / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / schema clarity`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for product changed/deleted after order creation
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：主要是狀態/欄位契約，有限矩陣測試較合適）
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [ ] Adversarial/penetration-style cases are added/updated for null snapshot / deleted product / replay edge cases
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only「worker completes」
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: 建單快照落庫
- Requirement mapping: `R1.x / CL-SNAPSHOT-01`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-persist-contract-fields`
- Reason: 風險集中在 schema / repository / service chain，不需要完整 Discord E2E

### Decision Record 2: 商品修改後付款 replay
- Requirement mapping: `R2.x-R3.x / CL-SNAPSHOT-02..03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-product-changed-after-order`
- Reason: 最重要的是驗證與 live product 脫鉤，整合測試足夠

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: 快照欄位與建單驗證
- Requirement mapping: `R1.x / Task 1 / CL-SNAPSHOT-01`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: snapshot replay 與文件對齊
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-SNAPSHOT-02..03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
