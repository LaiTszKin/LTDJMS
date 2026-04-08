# Checklist: Issue 74 設定 Schema 正規化

- Date: 2026-04-09
- Feature: Issue 74 設定 Schema 正規化

## Clarification & Approval Gate
- [ ] User clarification responses are recorded — `N/A`（目前需求已足夠明確）
- [ ] Affected plans are reviewed/updated — `N/A`（尚未進入 clarification update）
- [ ] Explicit user approval on updated specs is obtained（待本批 specs 審核）

## Behavior-to-Test Checklist

- [ ] CL-74-01 canonical config path 與 packaged defaults 使用同一套 key namespace
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-74-01`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 驗證 `application.properties`（以及若保留的 `application.conf`）不再承載漂移 schema

- [ ] CL-74-02 `.env`、packaged defaults、built-in defaults 的優先序與文件聲明一致
  - Requirement mapping: `R2.1-R2.3`, `R3.2`
  - Actual test case IDs: `UT-74-02`, `UT-74-03`
  - Test level: `Unit`
  - Risk class: `data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 需保留 malformed / empty `.env` 的容錯案例

- [ ] CL-74-03 文件描述與實際 canonical schema 不再矛盾
  - Requirement mapping: `R2.1-R2.3`, `R3.3`
  - Actual test case IDs: `DOC-74-01`
  - Test level: `Regression`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `NOT RUN`
  - Notes (optional): 至少要有人可從文件直接找出真正的 packaged defaults owner 與 key namespace

## Required Hardening Records
- [ ] Regression tests are added/updated for bug-prone or high-risk behavior
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [ ] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations, or `N/A` is recorded with a concrete reason
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: fallback chain correctness
- Requirement mapping: `R1.x-R3.2 / CL-74-01~02`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `EnvironmentConfigDotEnvIntegrationTest` 擴充後案例
- Reason: 風險集中於 config loading 與 getter fallback，單元／近整合測試即可精確驗證

### Decision Record 2: 文件同步
- Requirement mapping: `R2.1-R2.3 / CL-74-03`
- Decision: `N/A`
- Linked case IDs: `DOC-74-01`
- Reason: 文件同步不是 E2E 問題，但必須留下可回歸的審核檢查點

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
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-74-01~03`
- Completion status: `completed`
- Remaining applicable items: `Implementation and verification`
- Notes: 本 spec 明確指定 `EnvironmentConfig` 與 canonical packaged defaults 的權責，避免維運再碰到「檔案存在但 runtime 不讀」的配置陷阱

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `deferred`
- Remaining applicable items: `All implementation and test tasks`
- Notes: 本文件目前僅為待審批的 planning set
