# Checklist: Issue 75 貨幣持久層單一路徑化

- Date: 2026-04-09
- Feature: Issue 75 貨幣持久層單一路徑化

## Clarification & Approval Gate
- [ ] User clarification responses are recorded — `N/A`（目前需求已足夠明確）
- [ ] Affected plans are reviewed/updated — `N/A`（尚未進入 clarification update）
- [ ] Explicit user approval on updated specs is obtained（待本批 specs 審核）

## Behavior-to-Test Checklist

- [ ] CL-75-01 主要 integration / performance suites 改走 JOOQ account + config path
  - Requirement mapping: `R1.1-R1.2`
  - Actual test case IDs: `IT-75-01`, `PT-75-01`
  - Test level: `Integration / Performance`
  - Risk class: `regression`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `near-real dependency`
  - Oracle/assertion focus: `persisted state`
  - Test result: `NOT RUN`
  - Notes (optional): 需覆蓋主要 setup helper 與 service graph

- [ ] CL-75-02 `JooqGuildCurrencyConfigRepository` 具有與 account repository 對等的回歸覆蓋
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `IT-75-02`
  - Test level: `Integration`
  - Risk class: `data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `near-real dependency`
  - Oracle/assertion focus: `persisted state`
  - Test result: `NOT RUN`
  - Notes (optional): 覆蓋 save / find / update / saveOrUpdate / delete

- [ ] CL-75-03 JDBC account / config 不再作為主 regression 預設路徑
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `RG-75-01`
  - Test level: `Regression`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 若保留 transitional path，需有明確標記與清理說明

## Required Hardening Records
- [ ] Regression tests are added/updated for bug-prone or high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations, or `N/A` is recorded with a concrete reason
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures)

## E2E / Integration Decision Records

### Decision Record 1: repository owner alignment
- Requirement mapping: `R1.x-R2.x / CL-75-01~02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-75-01`, `IT-75-02`
- Reason: 風險在 repository 實作與資料映射，必須直接對真實 Postgres + JOOQ path 驗證

### Decision Record 2: transitional JDBC cleanup
- Requirement mapping: `R3.x / CL-75-03`
- Decision: `N/A`
- Linked case IDs: `RG-75-01`
- Reason: 這是結構與命名責任問題，不是使用者 E2E 流程；需留下回歸檢查點而非 UI 測試

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `N/A`
- [ ] Adversarial/penetration-style cases: `N/A`

## Completion Records

### Completion Record 1: 規格與設計
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-75-01~03`
- Completion status: `completed`
- Remaining applicable items: `Implementation and verification`
- Notes: 本 spec 以「production path 與主測試 path 對齊」為中心，不把 scope 擴張到 transaction repository 或 domain API 改寫

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `deferred`
- Remaining applicable items: `All implementation and test tasks`
- Notes: 本文件目前僅為待審批的 planning set
