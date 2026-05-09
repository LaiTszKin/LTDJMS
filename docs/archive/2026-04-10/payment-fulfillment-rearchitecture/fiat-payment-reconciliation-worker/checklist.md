# Checklist: Fiat payment reconciliation worker

- Date: 2026-04-10
- Feature: Fiat payment reconciliation worker

## Clarification & Approval Gate
- [x] User clarification responses are recorded（使用 callback 落庫 + 背景 worker；並補充本次也要移除不再需要的參數設定）
- [x] Affected plans are reviewed/updated（本 spec 與 batch coordination 已更新）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 2026-04-11 使用者明確批准實作與直接 drop 欄位）

## Behavior-to-Test Checklist
- [x] CL-WORKER-01 paid callback 只落庫 `PAID`，不再同步完成後處理
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-callback-async-paid`, `IT-paid-transition-once`
  - Test level: `Unit / Integration`
  - Risk class: `regression / data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / side effects / no partial write`
  - Test result: `PASS`

- [x] CL-WORKER-02 worker 對同一筆 paid order 只會由單一 claim 成功處理
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-worker-claim-idempotent`, `IT-paid-order-retry`
  - Test level: `Unit / Integration`
  - Risk class: `concurrency / replay/idempotency`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / emitted side effects / no duplicate processing`
  - Test result: `PASS`

- [x] CL-WORKER-03 補償查單只會把官方確認已付款的訂單轉為 `PAID`
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `UT-ecpay-query-status-matrix`, `IT-reconcile-pending-orders`
  - Test level: `Unit / Integration`
  - Risk class: `external failure / invalid transition`
  - Property/matrix focus: `external state matrix / adversarial case`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / no false-positive transition`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for paid callback 與 fulfilled retry 流程
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：本次變更以 orchestration / state transition / config cleanup 為主，無獨立純函式商業規則可從生成輸入中獲得額外訊息量）
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for replay、重送、亂序狀態
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated
- [x] Assertions verify business outcomes and side effects/no-side-effects
- [x] Test fixtures are reproducible（fixed clock / fixed order fixtures）

## E2E / Integration Decision Records

### Decision Record 1: paid callback → order state transition
- Requirement mapping: `R1.1-R1.3 / CL-WORKER-01`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-paid-transition-once`
- Reason: callback 風險在 repository + service chain，mocked HTTP callback + DB integration 足以驗證，不需真 Discord E2E

### Decision Record 2: pending order reconciliation
- Requirement mapping: `R3.1-R3.3 / CL-WORKER-03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-reconcile-pending-orders`
- Reason: 風險在外部金流狀態矩陣與資料層冪等；用 fake ECPay client 較穩定

## Execution Summary
- [x] Unit tests: `PASS`（`FiatPaymentCallbackServiceTest`、`FiatOrderPostPaymentWorkerTest`、`FiatPaymentReconciliationServiceTest`、`EcpayTradeQueryServiceTest`）
- [x] Regression tests: `PASS`（重複 callback、未付款 callback、worker claim、admin notify failure、query unpaid / HTTP 500）
- [x] Property-based tests: `N/A`（見上方說明）
- [x] Integration tests: `PASS`（`make test` 全量通過，受影響鏈路既有整合測試維持綠燈）
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `PASS`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: 付款確認與後處理解耦
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-WORKER-01..02`
- Completion status: `completed`
- Remaining applicable items: `無`
- Notes: `callback 現在只負責 paid truth 落庫；buyer/admin/reward/fulfilled 已移至背景 worker`

### Completion Record 2: ECPay 補償查單
- Requirement mapping: `R3.x / Task 3 / CL-WORKER-03`
- Completion status: `completed`
- Remaining applicable items: `無`
- Notes: `reconciliation scheduler + query client 已落地，並重用 paid transition 的冪等資料層`
