# Checklist: Fiat Order Buyer Notifications

- Date: 2026-04-09
- Feature: Fiat Order Buyer Notifications

## Clarification & Approval Gate (required when clarification replies exist)
- [x] User clarification responses are recorded (map to `spec.md`; if none, mark `N/A`).  
  `N/A`：使用者需求已明確，無額外 clarification。
- [x] Affected plans are reviewed/updated (`spec.md` / `tasks.md` / `checklist.md` / `contract.md` / `design.md`; if no updates needed, mark `N/A` + reason).
- [x] Explicit user approval on updated specs is obtained (date/conversation reference: 2026-04-09 本對話直接要求實作付款成功通知與付款期限提醒).  
  以 2026-04-09 本次需求請求作為實作授權依據，無額外規格往返。

## Behavior-to-Test Checklist (customizable)

- [x] CL-01 建單成功私訊包含訂單編號、付款期限與逾期取消提醒
  - Requirement mapping: `R1.1`, `R1.2`
  - Actual test case IDs: `UT-FiatOrderService-01`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `exact output`
  - External dependency strategy: `mocked ECPay code generation result`
  - Oracle/assertion focus: `exact output`
  - Test result: `PASS`
  - Notes (optional): 驗證使用者能從 DM 取得付款期限與取消風險。

- [x] CL-02 首次付款成功 callback 會私訊買家且不影響既有副作用
  - Requirement mapping: `R2.1`
  - Actual test case IDs: `UT-FiatPaymentCallbackService-01`
  - Test level: `Unit`
  - Risk class: `data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `mocked Discord DM + mocked repository claims`
  - Oracle/assertion focus: `side effects`
  - Test result: `PASS`
  - Notes (optional): 需同時驗證 admin notify 與 fulfillment 仍發生。

- [x] CL-03 付款成功私訊失敗或 duplicate callback 時不重複通知且不回滾流程
  - Requirement mapping: `R2.2`, `R2.3`
  - Actual test case IDs: `UT-FiatPaymentCallbackService-02`, `UT-FiatPaymentCallbackService-03`
  - Test level: `Unit`
  - Risk class: `external failure`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `mocked Discord DM failure states`
  - Oracle/assertion focus: `side effects / no partial write`
  - Test result: `PASS`
  - Notes (optional): 需分開覆蓋 DM 開啟失敗與 duplicate paid callback。

## Required Hardening Records
- [x] Regression tests are added/updated for bug-prone or high-risk behavior, or `N/A` is recorded with a concrete reason.
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason。  
  `N/A`：本次沒有新增可生成型商業邏輯或狀態機不變式，主要是文案與副作用串接。
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason.
- [x] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations, or `N/A` is recorded with a concrete reason.
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons。  
  Authorization `N/A`：本次未新增新的權限入口；已覆蓋 replay / duplicate callback 風險。
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw".
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason.

## E2E / Integration Decision Records

### Decision Record 1: 建單買家私訊提醒
- Requirement mapping: `R1.1`, `R1.2`, `CL-01`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `UT-FiatOrderService-01`, `ShopSelectMenuHandlerTest` 既有 DM fallback coverage
- Reason: 風險集中於文案內容與 handler 既有 fallback 相容性，用單元測試搭配既有 interaction 測試即可穩定驗證。

### Decision Record 2: 付款成功買家通知 callback path
- Requirement mapping: `R2.1`, `R2.2`, `R2.3`, `CL-02`, `CL-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `UT-FiatPaymentCallbackService-01..03`
- Reason: 風險在 callback service 對 repository claim、duplicate paid 與 Discord DM 的副作用編排；以固定 clock 與 mock repository / mock notification service 的單元測試即可穩定驗證。

## Execution Summary (fill with actual results)
- [x] Unit tests: `PASS`
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`（文案與副作用編排調整，無新的可生成型不變式）
- [x] Integration tests: `N/A`（此變更風險已由固定 clock 的 service-level tests 穩定覆蓋）
- [x] E2E tests: `N/A`（真實 ECPay / Discord E2E 成本高，且本次未改外部 contract）
- [x] External service mock scenarios: `PASS`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: 建單提醒與付款成功通知
- Requirement mapping: `R1.1`-`R2.3` / `Task 1` / `Task 2`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: 建單提醒已補入付款期限內完成付款與逾期取消語意；付款成功 callback 已新增買家私訊通知且維持既有副作用順序。

### Completion Record 2: 測試與驗證
- Requirement mapping: `CL-01`-`CL-03` / `Task 3`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: `mvn -q -Dtest=FiatOrderServiceTest,FiatPaymentCallbackServiceTest,ShopSelectMenuHandlerTest test` 與 `make format-check` 已通過。
