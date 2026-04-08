# Checklist: Issues 71-72 綠界 Callback 驗證強化

- Date: 2026-04-09
- Feature: Issues 71-72 綠界 Callback 驗證強化

## Clarification & Approval Gate
- [ ] User clarification responses are recorded — `N/A`（目前需求已足夠明確）
- [ ] Affected plans are reviewed/updated — `N/A`（尚未進入 clarification update）
- [ ] Explicit user approval on updated specs is obtained（待本批 specs 審核）

## Behavior-to-Test Checklist

- [ ] CL-71-01 建立綠界 ReturnURL 時不再附加 query token
  - Requirement mapping: `R1.1-R1.2`
  - Actual test case IDs: `UT-71-01`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `exact output`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證 legacy token expectation 已移除

- [ ] CL-71-02 `ECPAY_STAGE_MODE=true` + public bind 會在啟動階段 fail closed
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-71-02`, `IT-71-01`
  - Test level: `Unit / Integration`
  - Risk class: `authorization`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `permission denial`
  - Test result: `NOT RUN`
  - Notes (optional): 需涵蓋 `0.0.0.0`、public IP、loopback 對照矩陣

- [ ] CL-71-03 legit production callback、duplicate callback 與 validation failure 不回歸
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `UT-71-03`, `UT-71-04`
  - Test level: `Unit`
  - Risk class: `data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `persisted state`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證 mark-paid、duplicate callback、unpaid callback、merchant/amount mismatch

## Required Hardening Records
- [ ] Regression tests are added/updated for bug-prone or high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures)

## E2E / Integration Decision Records

### Decision Record 1: callback exposure policy
- Requirement mapping: `R2.x / CL-71-02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-71-01`
- Reason: 風險在 server 啟動條件與 HTTP endpoint 暴露，不需要完整外部 E2E，但需要一個近真的 server 啟動案例

### Decision Record 2: paid callback business path
- Requirement mapping: `R3.x / CL-71-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `FiatPaymentCallbackServiceTest` 擴充後案例
- Reason: 業務風險集中於 service 分支與 repository 協作，單元／近整合測試比真實 ECPay E2E 更穩定

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: 規格與設計
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-71-01~03`
- Completion status: `completed`
- Remaining applicable items: `Implementation and verification`
- Notes: 本 spec 將 query-token 移除與 stage/public fail-closed 綁在同一組 trust-boundary 調整中，避免兩個 issue 分開修卻互相留下漏洞

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `deferred`
- Remaining applicable items: `All implementation and test tasks`
- Notes: 本文件目前僅為待審批的 planning set
