# Checklist: Issue 70 履約 URL DNS Rebinding 強化

- Date: 2026-04-09
- Feature: Issue 70 履約 URL DNS Rebinding 強化

## Clarification & Approval Gate
- [ ] User clarification responses are recorded — `N/A`（目前需求已足夠明確）
- [ ] Affected plans are reviewed/updated — `N/A`（尚未進入 clarification update）
- [ ] Explicit user approval on updated specs is obtained（待本批 specs 審核）

## Behavior-to-Test Checklist

- [ ] CL-01 已驗證的 IP snapshot 會被 transport 直接使用，且不再以 hostname 決定 socket 目的地
  - Requirement mapping: `R1.1-R1.2`
  - Actual test case IDs: `UT-70-01`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 以 fake resolver + mocked transport 驗證 `ResolvedTarget.resolvedAddress`

- [ ] CL-02 localhost / RFC1918 / special-use / unknown host 會在送出前被拒絕
  - Requirement mapping: `R2.1`
  - Actual test case IDs: `UT-70-02`
  - Test level: `Unit`
  - Risk class: `adversarial abuse`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `permission denial`
  - Test result: `NOT RUN`
  - Notes (optional): 包含 IPv4 special-use 與 IPv6 ULA / link-local

- [ ] CL-03 non-2xx 與 transport failure 不會留下 fulfillment 成功副作用
  - Requirement mapping: `R2.2-R2.3`
  - Actual test case IDs: `UT-70-03`
  - Test level: `Unit`
  - Risk class: `external failure`
  - Property/matrix focus: `adversarial case`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `no partial write`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證 redirect-like / timeout / TLS 失敗時回傳 `DomainError`

## Required Hardening Records
- [ ] Regression tests are added/updated for bug-prone or high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures)

## E2E / Integration Decision Records

### Decision Record 1: DNS rebinding / target snapshot
- Requirement mapping: `R1.1-R1.2 / CL-01`
- Decision: `Cover with integration instead`
- Linked case IDs: `UT-70-01`, `IT-70-01`
- Reason: 核心風險在 internal boundary，不需要完整 E2E；必要時以近真 transport 測試補一個 integration case 即可

### Decision Record 2: non-public target rejection
- Requirement mapping: `R2.1 / CL-02`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `ProductFulfillmentApiServiceTest` 擴充後案例
- Reason: 風險集中於 validation function，單元測試比 E2E 更穩定、更能覆蓋特殊位址矩陣

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
- Requirement mapping: `R1.x-R2.x / Task 1-3 / CL-01~03`
- Completion status: `completed`
- Remaining applicable items: `Implementation and verification`
- Notes: 已確認 issue #70 的核心風險在現行程式中已被部分緩解，因此本 spec 以「固化與回歸保護」為主

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `deferred`
- Remaining applicable items: `All implementation and test tasks`
- Notes: 本文件目前僅為待審批的 planning set
