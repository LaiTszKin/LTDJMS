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

## Clarification & Approval Gate (required when clarification replies exist)
- [ ] User clarification responses are recorded (map to `spec.md`; if none, mark `N/A`).
- [ ] Affected plans are reviewed/updated (`spec.md` / `tasks.md` / `checklist.md` / `contract.md` / `design.md`; if no updates needed, mark `N/A` + reason).
- [ ] Explicit user approval on updated specs is obtained (date/conversation reference: [to be filled]).

## Behavior-to-Test Checklist (customizable)

- [ ] CL-01 新買家通知服務能正確透過 DiscordRuntimeGateway 發送 DM
  - Requirement mapping: R3, R4
  - Actual test case IDs: UT-01
  - Test level: Unit
  - Risk class: external failure
  - Property/matrix focus: mocked user retrieval + DM channel
  - External dependency strategy: mocked DiscordRuntimeGateway
  - Oracle/assertion focus: verify `retrieveUserById(customerUserId)` and `sendMessage()` are called
  - Unit drift check: UT-01 target unit: `EscortOrderBuyerNotificationService.notifyEscortOrderCreated()`; assertions: `retrieveUserById` called with correct `customerUserId`, message sent to private channel
  - Test result: `NOT RUN`
  - Notes (optional):

- [ ] CL-02 買家通知訊息格式正確（不同 sourceType 顯示對應付款方式）
  - Requirement mapping: R3.2, R4.2
  - Actual test case IDs: UT-02
  - Test level: Unit
  - Risk class: boundary (message format)
  - Property/matrix focus: CURRENCY_PURCHASE → 顯示「貨幣」; FIAT_PAYMENT → 顯示「法幣」
  - External dependency strategy: none (純字串比對)
  - Oracle/assertion focus: 訊息包含「商品名稱」、護航訂單編號、「等待處理」、對應付款描述
  - Unit drift check: UT-02 target unit: `EscortOrderBuyerNotificationService.buildEscortOrderCreatedMessage()`; assertions: message contains keywords, CURRENCY_PURCHASE message says "貨幣", FIAT_PAYMENT message says "法幣"
  - Test result: `NOT RUN`
  - Notes (optional):

- [ ] CL-03 貨幣購買流程在 handoff 成功後呼叫買家通知
  - Requirement mapping: R3.1
  - Actual test case IDs: UT-03
  - Test level: Unit
  - Risk class: regression (integration point)
  - Property/matrix focus: handoff success → notification called; handoff failure → notification not called
  - External dependency strategy: mocked services
  - Oracle/assertion focus: verify `escortOrderBuyerNotificationService.notifyEscortOrderCreated()` is called exactly when handoff succeeds, not when it fails
  - Unit drift check: UT-03 target unit: `ShopSelectMenuHandler.onButtonInteraction()` escort branch; assertions: notification called on Result.ok, not called on Result.err
  - Test result: `NOT RUN`
  - Notes (optional):

- [ ] CL-04 法幣付款流程在 handoff 成功後呼叫買家通知
  - Requirement mapping: R4.1
  - Actual test case IDs: UT-04
  - Test level: Unit
  - Risk class: regression (integration point)
  - Property/matrix focus: handoff success → notification called after admin notification
  - External dependency strategy: mocked services
  - Oracle/assertion focus: verify call order: buyerPaymentNotification → escortHandoff → buyerEscortNotification → adminNotification
  - Unit drift check: UT-04 target unit: `FiatOrderPostPaymentWorker.processSingleOrder()`; assertions: `escortOrderBuyerNotificationService.notifyEscortOrderCreated()` called with correct dispatch order after successful handoff
  - Test result: `NOT RUN`
  - Notes (optional):

- [ ] CL-05 管理員通知含明確付款狀態行
  - Requirement mapping: R1.2, R2.2
  - Actual test case IDs: UT-05
  - Test level: Unit
  - Risk class: boundary (message format)
  - Property/matrix focus: CURRENCY_PURCHASE / FIAT_PAYMENT source type
  - External dependency strategy: none (字串比對)
  - Oracle/assertion focus: 訊息包含 `付款狀態：` 行; CURRENCY_PURCHASE → `已付款（貨幣）`; FIAT_PAYMENT → `已付款（法幣）`
  - Unit drift check: UT-05 target unit: `ShopAdminNotificationService.buildAdminOrderNotification(Guild, long, EscortDispatchOrder)`; assertions: message contains payment status line with correct format
  - Test result: `NOT RUN`
  - Notes (optional):

- [ ] CL-06 買家 DM 關閉時不拋例外、僅 log warn
  - Requirement mapping: Error and Edge Cases
  - Actual test case IDs: UT-06
  - Test level: Unit
  - Risk class: external failure
  - Property/matrix focus: mock failure in openPrivateChannel / sendMessage
  - External dependency strategy: mocked failure callbacks
  - Oracle/assertion focus: no exception propagates to caller; warn log is recorded
  - Unit drift check: UT-06 target unit: `EscortOrderBuyerNotificationService.notifyEscortOrderCreated()`; assertions: method completes without throwing when DM channel fails
  - Test result: `NOT RUN`
  - Notes (optional):

## Required Hardening Records
- [x] Regression tests are added/updated for bug-prone or high-risk behavior — `N/A` 通知邏輯為 additive change，不修改既有核心業務邏輯
- [x] Focused unit drift checks are defined for non-trivial atomic implementation tasks — 已定義 UT-01 至 UT-06，涵蓋所有新功能
- [x] Property-based coverage is added/updated for changed business logic — `N/A` 此變更僅為通知發送，無複雜業務邏輯需要 PBT
- [x] External services in the business logic chain are mocked/faked for scenario testing — 已計畫透過 mock DiscordRuntimeGateway 測試
- [x] Adversarial/penetration-style cases are added/updated for abuse paths — `N/A` 通知功能無 abuse 路徑（無權限提升、資料暴露風險）
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated — `N/A` 通知為 fire-and-forget、不改變狀態、不影響既有流程
- [x] Assertions verify business outcomes and side effects/no-side-effects — UT 驗證 DM 發送時機與內容，不影響既有購買/付款行為
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) — 使用 Mockito 固定 mock 行為

## E2E / Integration Decision Records

### Decision Record 1: 貨幣購買護航通知整合測試
- Requirement mapping: R1, R3
- Decision: Cover with integration instead
- Linked case IDs: 現有 `ShopSelectMenuHandlerTest` — 擴充測試方法
- Reason: 通知為 side effect，無法透過目前整合測試框架驗證 Discord DM 實際送達；單元測試已夠驗證呼叫順序與參數

### Decision Record 2: 法幣付款護航通知整合測試
- Requirement mapping: R2, R4
- Decision: Cover with integration instead
- Linked case IDs: 現有 `FiatOrderPostPaymentWorkerTest` — 擴充測試方法
- Reason: 同 Decision Record 1，單元測試已夠驗證呼叫順序與參數；現有整合測試 `FiatOrderPostPaymentWorkerIntegrationTest` 不需修改

## Execution Summary (fill with actual results)
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `N/A`

## Completion Records

### Completion Record 1: 買家護航通知
- Requirement mapping: R3, R4 / Task 1, 2, 3
- Completion status: [not started]
- Remaining applicable items: [待實作]
- Notes:

### Completion Record 2: 管理員通知強化
- Requirement mapping: R1, R2 / Task 4
- Completion status: [not started]
- Remaining applicable items: [待實作]
- Notes:
