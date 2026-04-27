# Checklist: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：issue 與現況證據已足夠）
- [x] Affected plans are reviewed/updated（已更新本 spec 與 batch 內 shared coordination 說明）
- [x] Explicit user approval on updated specs is obtained（本次依既有 spec 直接實作，user follow-up: `continue`）

## Behavior-to-Test Checklist
- [x] CL-SNAPSHOT-01 建單時會寫入完整履約快照
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-fiat-order-create-snapshot`, `IT-persist-contract-fields`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / regression`
  - Property/matrix focus: `field matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `persisted snapshot / validation failure`
  - Test result: `PASS`

- [x] CL-SNAPSHOT-02 paid worker 只 replay order snapshot
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-worker-snapshot-only`, `IT-product-changed-after-order`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / replay-idempotency`
  - Property/matrix focus: `state replay matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `no live-product dependency / no duplicate side effect`
  - Test result: `PASS`

- [x] CL-SNAPSHOT-03 文件與命名顯示 order 是履約契約 owner
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `schema review`
  - Test level: `Review`
  - Risk class: `regression / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / schema clarity`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for product changed/deleted after order creation
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：此變更是有限欄位契約與 replay matrix，使用矩陣型 unit/integration 更能直接覆蓋風險）
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [x] Adversarial/penetration-style cases are added/updated for null snapshot / deleted product / replay edge cases
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons（N/A：沒有新增權限邊界；replay/idempotency 已由 claim / mark tests 覆蓋；並發風險沿用既有 claim lock）
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only「worker completes」
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `PASS`
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`
- [x] Integration tests: `PASS`
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `PASS`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: 快照欄位與建單驗證
- Requirement mapping: `R1.x / Task 1 / CL-SNAPSHOT-01`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `已落地新增快照欄位、建單驗證與 repository persistence`

### Completion Record 2: snapshot replay 與文件對齊
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-SNAPSHOT-02..03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `worker 已改為 snapshot replay，文件與命名同步更新`
