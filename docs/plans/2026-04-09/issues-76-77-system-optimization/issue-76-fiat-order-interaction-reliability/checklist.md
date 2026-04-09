# Checklist: Issue 76 法幣下單互動可靠性

- Date: 2026-04-09
- Feature: Issue 76 法幣下單互動可靠性

## Clarification & Approval Gate
- [x] User clarification responses are recorded — `APP_PUBLIC_BASE_URL` 與「只做互動層防重入、不做未付款訂單重用」已體現在 batch coordination 與 spec 中
- [x] Affected plans are reviewed/updated — `spec.md` / `tasks.md` / `checklist.md` / `contract.md` / `design.md`
- [x] Explicit user approval on updated specs is obtained（使用者已要求「更新計劃然後實作」）

## Behavior-to-Test Checklist

- [x] CL-76-01 法幣下單會先 deferred reply，再透過 hook 回填結果
  - Requirement mapping: `R1.1-R1.2`
  - Actual test case IDs: `UT-76-01`, `UT-76-02`
  - Test level: `Unit`
  - Risk class: `external failure`
  - Property/matrix focus: `interaction lifecycle`
  - External dependency strategy: `mocked Discord interaction`
  - Oracle/assertion focus: `side effects`
  - Test result: `PASS`
  - Notes (optional): 驗證不再直接 `event.reply(...)`

- [x] CL-76-02 成功時 interaction 內保留訂單摘要與查詢提示
  - Requirement mapping: `R2.1-R2.2`
  - Actual test case IDs: `UT-76-03`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `exact output`
  - External dependency strategy: `mocked Discord DM`
  - Oracle/assertion focus: `exact output`
  - Test result: `PASS`
  - Notes (optional): 需涵蓋 DM 成功但 interaction 仍保留摘要

- [x] CL-76-03 DM 失敗時 interaction 內顯示完整付款備援資訊
  - Requirement mapping: `R3.1`
  - Actual test case IDs: `UT-76-04`, `UT-76-05`
  - Test level: `Unit`
  - Risk class: `external failure`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `mocked Discord DM failure states`
  - Oracle/assertion focus: `exact output`
  - Test result: `PASS`
  - Notes (optional): 需分開覆蓋 open DM 失敗與 send DM 失敗

- [x] CL-76-04 同一商品的重複互動不會造成第二次取號
  - Requirement mapping: `R3.2-R3.3`
  - Actual test case IDs: `UT-76-06`
  - Test level: `Unit`
  - Risk class: `concurrency`
  - Property/matrix focus: `in-flight guard`
  - External dependency strategy: `mocked deferred reply pending state`
  - Oracle/assertion focus: `no side effects`
  - Test result: `PASS`
  - Notes (optional): 驗證第二次互動直接回「處理中」且不呼叫 `FiatOrderService`

## Required Hardening Records
- [x] Regression tests are added/updated for bug-prone or high-risk behavior
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason — `N/A`，本次僅調整 interaction 協調與訊息組裝，未新增可生成測試空間的業務規則
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations, or `N/A` is recorded with a concrete reason — 已以重複點擊同商品的重入測試覆蓋主要濫用路徑
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons — authorization / invalid transition `N/A`，既有權限與狀態流轉未變；concurrency 已覆蓋 in-flight guard
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures)

## E2E / Integration Decision Records

### Decision Record 1: Discord interaction lifecycle
- Requirement mapping: `R1.x-R3.x / CL-76-01~04`
- Decision: `Cover with integration instead`
- Linked case IDs: `ShopSelectMenuHandlerTest`
- Reason: 風險集中在 handler 的 JDA 互動協調與 callback chaining，unit + mocked interaction 比端到端 Discord 測試穩定且足夠

### Decision Record 2: ECPay / repository business path
- Requirement mapping: `R2.x / CL-76-02`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `FiatOrderServiceTest`
- Reason: 本 spec 不改動 ECPay payload 與 repository state transition，保留既有服務層 coverage 即可

## Execution Summary
- [x] Unit tests: `PASS`
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`
- [x] Integration tests: `N/A`
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `PASS`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: 規格與設計
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-76-01~04`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: 本 spec 已收斂成「deferred reply + 摘要 fallback + 互動層防重入」，不含資料層 dedupe

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: `ShopSelectMenuHandlerTest` 已覆蓋 deferred reply、成功摘要、DM fallback、guard 釋放與處理中提示；測試通過
