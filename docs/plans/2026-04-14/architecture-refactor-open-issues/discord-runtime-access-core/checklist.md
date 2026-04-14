# Checklist: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與 JDA lifecycle 證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若 gateway 命名或範圍改動，需同步 coordination 與 callsite spec）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-RUNTIME-01 runtime gateway 正式成為唯一 core owner
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-runtime-gateway-ready-state`, `IT-bootstrap-publishes-runtime`
  - Test level: `Unit / Integration`
  - Risk class: `regression / lifecycle`
  - Property/matrix focus: `ready-state invariant`
  - External dependency strategy: `mocked JDA lifecycle`
  - Oracle/assertion focus: `explicit readiness / no global singleton dependency`
  - Test result: `NOT RUN`

- [ ] CL-RUNTIME-02 compatibility bridge 可支撐漸進遷移但不成為長期 owner
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-bridge-transitional-only`, `UT-no-new-static-dependency-helper`
  - Test level: `Unit`
  - Risk class: `maintainability / regression`
  - Property/matrix focus: `boundary invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `restricted surface / test isolation`
  - Test result: `NOT RUN`

- [ ] CL-RUNTIME-03 文件、wiring 與 bootstrap 描述一致
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `wiring review`
  - Test level: `Review`
  - Risk class: `diagnosability / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / wiring clarity`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for bootstrap-ready lifecycle and bridge guardrails
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：主要是 lifecycle abstraction，不是高維純函式規則）
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for not-ready access / partial publish / stale bridge paths
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only object non-null
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: bootstrap publishes runtime
- Requirement mapping: `R1.x / CL-RUNTIME-01`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-bootstrap-publishes-runtime`
- Reason: 需驗證 bootstrap 與 DI graph 的實際配合

### Decision Record 2: abstraction/documentation parity
- Requirement mapping: `R2.x-R3.x / CL-RUNTIME-02..03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `unit tests + doc review`
- Reason: 無需建立真 Discord E2E

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: runtime gateway 與 bridge
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-RUNTIME-01..02`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: 文件與 wiring 對齊
- Requirement mapping: `R3.x / Task 3 / CL-RUNTIME-03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
