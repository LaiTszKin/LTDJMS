# Checklist: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：issue 與 JDA lifecycle 證據已足夠）
- [x] Affected plans are reviewed/updated（已同步 `spec.md` / `tasks.md` / `checklist.md` / `contract.md` / `design.md`）
- [x] Explicit user approval on updated specs is obtained（本次使用者已直接要求 `implement-specs-with-worktree`）

## Behavior-to-Test Checklist
- [x] CL-RUNTIME-01 runtime gateway 正式成為唯一 core owner
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-runtime-gateway-ready-state`, `IT-bootstrap-publishes-runtime`
  - Test level: `Unit / Integration`
  - Risk class: `regression / lifecycle`
  - Property/matrix focus: `ready-state invariant`
  - External dependency strategy: `mocked JDA lifecycle`
  - Oracle/assertion focus: `explicit readiness / no global singleton dependency`
  - Test result: `PASS`

- [x] CL-RUNTIME-02 compatibility bridge 可支撐漸進遷移但不成為長期 owner
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-bridge-transitional-only`, `UT-no-new-static-dependency-helper`
  - Test level: `Unit`
  - Risk class: `maintainability / regression`
  - Property/matrix focus: `boundary invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `restricted surface / test isolation`
  - Test result: `PASS`

- [x] CL-RUNTIME-03 文件、wiring 與 bootstrap 描述一致
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `wiring review`
  - Test level: `Review`
  - Risk class: `diagnosability / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / wiring clarity`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for bootstrap-ready lifecycle and bridge guardrails
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（`N/A`：此變更為 lifecycle abstraction/refactor，不是高維 business rule；已用單元與整合測試覆蓋 ready/not-ready 與 duplicate publish invariant）
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for not-ready access / partial publish / stale bridge paths
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only object non-null
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `mvn -q -Punit-tests test` `PASS`
- [x] Regression tests: `mvn -q -Punit-tests test` `PASS`
- [x] Property-based tests: `N/A`（lifecycle abstraction 不是高維 business logic）
- [x] Integration tests: `mvn -q -Pintegration-tests test` `PASS`
- [x] E2E tests: `N/A`（此變更只影響 runtime access boundary 與 bootstrap wiring，無獨立 Discord 使用者 E2E path）
- [x] External service mock scenarios: `PASS`（JDA / guild / channel / self user 全部以 mock 驗證）
- [x] Adversarial/penetration-style cases: `PASS`（not-ready access、duplicate publish）

## Completion Records

### Completion Record 1: runtime gateway 與 bridge
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-RUNTIME-01..02`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `gateway、bridge、測試與 wiring 已完成`

### Completion Record 2: 文件與 wiring 對齊
- Requirement mapping: `R3.x / Task 3 / CL-RUNTIME-03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `docs/modules/discord-api-abstraction.md` 與 Dagger/bootstrap 實作已對齊
