# Checklist: Escort order handoff

- Date: 2026-04-14
- Feature: Escort order handoff

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 內容已足夠定義 handoff 目標）
- [ ] Affected plans are reviewed/updated（approval 後若調整欄位命名，需同步更新本 batch coordination）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [x] CL-DISPATCH-01 購買完成會建立單一 dispatch durable record
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-escort-handoff-idempotent`, `IT-auto-escort-order-created`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / replay-idempotency / regression`
  - Property/matrix focus: `invariant / event replay matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state / no duplicate record`
  - Test result: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceTest,DispatchPanelMessageFactoryTest,FiatOrderPostPaymentWorkerTest,ShopSelectMenuHandlerTest test`)

- [x] CL-DISPATCH-02 Dispatch record 會保存來源購買快照
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-dispatch-source-snapshot`, `IT-product-delete-after-handoff`
  - Test level: `Unit / Integration`
  - Risk class: `data integrity / regression`
  - Property/matrix focus: `state snapshot matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted snapshot / independence from live product row`
  - Test result: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceIntegrationTest,FiatOrderPostPaymentWorkerIntegrationTest test`)

- [x] CL-DISPATCH-03 管理員通知只在 handoff 成功後發送
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `UT-notify-after-handoff`, `IT-dm-failure-does-not-rollback`
  - Test level: `Unit / Integration`
  - Risk class: `side effect ordering / regression`
  - Property/matrix focus: `ordering invariant`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `side effects / no rollback of durable state`
  - Test result: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceIntegrationTest,FiatOrderPostPaymentWorkerIntegrationTest test`)

## Required Hardening Records
- [x] Regression tests are added/updated for purchase → dispatch handoff flows
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（`N/A`：核心風險在 idempotent orchestration，不是高維純函式規則）
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for replay paths and deleted-product edge cases
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only「method called」
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: purchase completion → dispatch handoff
- Requirement mapping: `R1.x-R2.x / CL-DISPATCH-01..02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-auto-escort-order-created`
- Reason: 風險在 schema、repository 與 service chain，真 Discord E2E 價值不高

### Decision Record 2: DM side effect ordering
- Requirement mapping: `R3.x / CL-DISPATCH-03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-dm-failure-does-not-rollback`
- Reason: 核心是 durable state 與 side effect ordering，不需要外部平台 E2E

## Execution Summary
- [x] Unit tests: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceTest,DispatchPanelMessageFactoryTest,FiatOrderPostPaymentWorkerTest,ShopSelectMenuHandlerTest test`)
- [x] Regression tests: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceTest,DispatchPanelMessageFactoryTest,FiatOrderPostPaymentWorkerTest,ShopSelectMenuHandlerTest test`)
- [x] Property-based tests: `N/A`（核心風險在 service orchestration、idempotency 與 persistence snapshot，不是高維純函式輸入空間）
- [x] Integration tests: `PASS` (`mvn -Dtest=EscortDispatchHandoffServiceIntegrationTest,FiatOrderPostPaymentWorkerIntegrationTest test`)
- [x] E2E tests: `N/A`（風險主要落在 schema / repository / service chain，整合測試已足夠覆蓋）
- [x] External service mock scenarios: `PASS`（`ShopAdminNotificationService` / `FiatOrderBuyerNotificationService` 在單元與整合測試中以 mock 取代）
- [x] Adversarial/penetration-style cases: `PASS`（duplicate source reference、deleted product、DM failure）

## Completion Records

### Completion Record 1: durable handoff 與 aggregate 擴充
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-DISPATCH-01..02`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `已完成 handoff boundary、dispatch snapshot、idempotency 與回歸測試`

### Completion Record 2: 通知降級與文件對齊
- Requirement mapping: `R3.x / Task 3 / CL-DISPATCH-03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `已完成通知降級、失敗保留 durable state 與文件回填`
