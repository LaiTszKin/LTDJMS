# Checklist: 護航購買通知

- Date: 2026-04-28
- Feature: 護航購買通知

## Usage Notes
- This checklist is a starter template. Add, remove, or rewrite items based on actual scope.
- Use `- [ ]` for all items; mark completed items as `- [x]`.
- The final completion summary section may use structured placeholders instead of checkboxes.
- If an item is not applicable, keep `N/A` with a concrete reason.
- Do not mark placeholder examples or mutually exclusive alternatives as completed unless they were actually selected and executed.
- Duplicate or remove decision-record blocks as needed; the final document should contain as many records as the real change requires.
- Duplicate or remove completion-record blocks as needed; the final document should contain as many records as the real change requires.
- Suggested test result values: `PASS / FAIL / BLOCKED / NOT RUN / N/A`.
- Use `$test-case-strategy` to choose test levels, define meaningful oracles, and record unit drift checks for atomic tasks.
- For business-logic changes, property-based coverage is required unless a concrete `N/A` reason is recorded.
- Each checklist item should map to a distinct risk; avoid repeating shallow happy-path cases.

## Clarification & Approval Gate
- [x] N/A — 規格階段無需釐清
- [x] All spec files are backfilled — spec.md、tasks.md、checklist.md、contract.md、design.md
- [x] User approval obtained via /implement-specs invocation (2026-04-28)

## Behavior-to-Test Checklist

- [x] CL-01 新買家通知服務能正確透過 DiscordRuntimeGateway 發送 DM
  - Requirement mapping: R3, R4
  - Actual test case IDs: UT-01
  - Test level: Unit
  - Risk class: external failure
  - Property/matrix focus: mocked user retrieval + DM channel
  - External dependency strategy: mocked DiscordRuntimeGateway
  - Oracle/assertion focus: verify `retrieveUserById(customerUserId)` and `sendMessage()` are called
  - Unit drift check: UT-01 — 通過
  - Test result: `PASS`
  - Notes (optional):

- [x] CL-02 買家通知訊息格式正確（不同 sourceType 顯示對應付款方式）
  - Requirement mapping: R3.2, R4.2
  - Actual test case IDs: UT-02
  - Test level: Unit
  - Risk class: boundary (message format)
  - Property/matrix focus: CURRENCY_PURCHASE → 顯示「貨幣」; FIAT_PAYMENT → 顯示「法幣」
  - External dependency strategy: none (純字串比對)
  - Oracle/assertion focus: 訊息包含「商品名稱」、護航訂單編號、「等待處理」、對應付款描述
  - Unit drift check: UT-02 — 通過
  - Test result: `PASS`
  - Notes (optional):

- [x] CL-03 貨幣購買流程在 handoff 成功後呼叫買家通知
  - Requirement mapping: R3.1
  - Actual test case IDs: UT-03
  - Test level: Unit
  - Risk class: regression (integration point)
  - Property/matrix focus: handoff success → notification called; handoff failure → notification not called
  - External dependency strategy: mocked services
  - Oracle/assertion focus: verify `escortOrderBuyerNotificationService.notifyEscortOrderCreated()` is called exactly when handoff succeeds, not when it fails
  - Unit drift check: UT-03 — 通過
  - Test result: `PASS`
  - Notes (optional):

- [x] CL-04 法幣付款流程在 handoff 成功後呼叫買家通知
  - Requirement mapping: R4.1
  - Actual test case IDs: UT-04
  - Test level: Unit
  - Risk class: regression (integration point)
  - Property/matrix focus: handoff success → notification called; handoff failure → notification not called
  - External dependency strategy: mocked services
  - Oracle/assertion focus: verify call order: buyerPaymentNotification → escortHandoff → buyerEscortNotification → adminNotification
  - Unit drift check: UT-04 — 通過
  - Test result: `PASS`
  - Notes (optional):

- [x] CL-05 管理員通知含明確付款狀態行
  - Requirement mapping: R1.2, R2.2
  - Actual test case IDs: UT-05
  - Test level: Unit
  - Risk class: boundary (message format)
  - Property/matrix focus: CURRENCY_PURCHASE / FIAT_PAYMENT source type + MANUAL
  - External dependency strategy: none (字串比對)
  - Oracle/assertion focus: 訊息包含 `付款狀態：` 行; 各 sourceType 對應正確描述
  - Unit drift check: UT-05 — 通過
  - Test result: `PASS`
  - Notes (optional):

- [x] CL-06 買家 DM 關閉時不拋例外、僅 log warn
  - Requirement mapping: Error and Edge Cases
  - Actual test case IDs: UT-06
  - Test level: Unit
  - Risk class: external failure
  - Property/matrix focus: mock failure in openPrivateChannel
  - External dependency strategy: mocked failure callbacks
  - Oracle/assertion focus: no exception propagates to caller
  - Unit drift check: UT-06 — 通過
  - Test result: `PASS`
  - Notes (optional):

## Required Hardening Records
- [x] Regression tests are added/updated for bug-prone or high-risk behavior — `N/A` 通知邏輯為 additive change，不修改既有核心業務邏輯
- [x] Focused unit drift checks are defined for non-trivial atomic implementation tasks — UT-01 至 UT-06 已全數實現並通過
- [x] Property-based coverage is added/updated for changed business logic — `N/A` 此變更僅為通知發送，無複雜業務邏輯需要 PBT
- [x] External services in the business logic chain are mocked/faked for scenario testing — 透過 mock DiscordRuntimeGateway 完成測試
- [x] Adversarial/penetration-style cases are added/updated for abuse paths — `N/A` 通知功能無 abuse 路徑
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated — `N/A` 通知為 fire-and-forget、不改變狀態
- [x] Assertions verify business outcomes and side effects/no-side-effects — UT 驗證 DM 發送時機與內容
- [x] Test fixtures are reproducible — 全部使用 Mockito 固定 mock

## E2E / Integration Decision Records

### Decision Record 1: 貨幣購買護航通知整合測試
- Requirement mapping: R1, R3
- Decision: Cover with unit tests instead
- Linked case IDs: `ShopSelectMenuHandlerTest` — 擴充 3 個測試方法
- Reason: 通知為 side effect，單元測試已驗證呼叫順序與參數；E2E 無法自動驗證 Discord DM 送達

### Decision Record 2: 法幣付款護航通知整合測試
- Requirement mapping: R2, R4
- Decision: Cover with unit tests instead
- Linked case IDs: `FiatOrderPostPaymentWorkerTest` — 擴充測試方法
- Reason: 同 Decision Record 1

## Execution Summary
- [x] Unit tests: `PASS` — 40 tests, 0 failures, 0 errors
- [x] Regression tests: `PASS` — 既有測試案例全部保持通過
- [x] Property-based tests: `N/A`
- [x] Integration tests: `NOT RUN` — 整合測試因需要 DB 未執行，但未修改整合測試依賴
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `PASS` — UT-01, UT-03, UT-04, UT-06 使用 mock DiscordRuntimeGateway
- [x] Adversarial/penetration-style cases: `N/A`

## Completion Records

### Completion Record 1: 買家護航通知（含貨幣購買 + 法幣付款）
- Requirement mapping: R3, R4 / Task 1, 2, 3
- Completion status: completed
- Remaining applicable items: None
- Notes: 新增 `EscortOrderBuyerNotificationService`，整合到 `ShopSelectMenuHandler`（貨幣）與 `FiatOrderPostPaymentWorker`（法幣）

### Completion Record 2: 管理員通知強化
- Requirement mapping: R1, R2 / Task 4
- Completion status: completed
- Remaining applicable items: None
- Notes: 在 `ShopAdminNotificationService.buildAdminOrderNotification()` 中增加顯式「付款狀態」行，支援三種 sourceType（CURRENCY_PURCHASE、FIAT_PAYMENT、MANUAL）
