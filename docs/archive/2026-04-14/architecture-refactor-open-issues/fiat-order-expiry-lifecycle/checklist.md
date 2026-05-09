# Checklist: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：issue 與上游/本地證據已足夠）
- [x] Affected plans are reviewed/updated（approval 後若 terminal status 命名改動，需同步 snapshot spec 與 coordination）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 本輪使用者已透過 spec path request implementation）

## Behavior-to-Test Checklist
- [x] CL-EXPIRY-01 未付款訂單會在期限後轉成 terminal state
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-expire-transition`, `IT-order-expires-after-deadline`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / regression`
  - Property/matrix focus: `time-state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / no lingering pending`
  - Test result: `PASS`

- [x] CL-EXPIRY-02 reconciliation 只處理仍在有效期內的 pending order
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-reconciliation-selection`, `IT-reconciliation-excludes-expired`, `UT-paid-vs-expired-race`
  - Test level: `Unit / Integration`
  - Risk class: `concurrency / external failure / invalid transition`
  - Property/matrix focus: `external state matrix / race invariant`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `single final truth / no duplicate transition`
  - Test result: `PASS`

- [x] CL-EXPIRY-03 文案與查詢會顯示 expired/cancelled 語意
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `UT-direct-message-expiry-wording`, `DOC-review`
  - Test level: `Unit / Review`
  - Risk class: `regression / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / buyer-facing copy`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for unpaid-expiry and near-deadline paid cases
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：時間/狀態矩陣有限，離散測試更直觀）
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for race、重試與缺少 expireAt 的歷史資料
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only query method 被呼叫
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `mvn -q -Dtest=FiatOrderTest,EcpayCvsPaymentServiceTest,FiatOrderServiceTest,FiatPaymentReconciliationServiceTest,FiatPaymentCallbackServiceTest,FiatOrderPostPaymentWorkerTest,ShopSelectMenuHandlerTest test` PASS
- [x] Regression tests: `UT-expire-transition`, `UT-reconciliation-selection`, `UT-paid-vs-expired-race`, `UT-direct-message-expiry-wording` PASS
- [x] Property-based tests: `N/A` - finite time/state matrix is covered more directly by unit and integration cases
- [x] Integration tests: `mvn -q -Dtest=JdbcFiatOrderRepositoryIntegrationTest test` PASS
- [x] E2E tests: `N/A` - existing integration coverage exercises the relevant DB transition and race surfaces without an unstable external dependency
- [x] External service mock scenarios: `EcpayCvsPaymentServiceTest`, `FiatPaymentCallbackServiceTest`, `FiatPaymentReconciliationServiceTest` PASS
- [x] Adversarial/penetration-style cases: `expiredPendingOrder`, `expiredOrder`, and race-loss release assertions PASS

## Completion Records

### Completion Record 1: terminal state 與 expiry transition
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-EXPIRY-01..02`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `EXPIRED terminal state, expiry sweep, and race handling are implemented and tested`

### Completion Record 2: 文案、查詢與文件對齊
- Requirement mapping: `R3.x / Task 3 / CL-EXPIRY-03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `buyer-facing copy, docs, and tests are aligned to explicit expiry semantics`
