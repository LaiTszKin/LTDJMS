# Checklist: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與接線證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若 canonical owner 選擇改變，需同步五份文檔與 coordination）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-MEM-01 只有單一 runtime ChatMemoryProvider 是 canonical owner
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-chat-memory-canonical-path`, `UT-module-wiring-memory-provider`
  - Test level: `Unit`
  - Risk class: `regression / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `single wiring path / no shadow provider`
  - Test result: `NOT RUN`

- [ ] CL-MEM-02 runtime memory 與 audit/legacy artifacts 分責清楚
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-legacy-provider-not-runtime`, `IT-migration-audit-repurpose-or-drop`
  - Test level: `Unit / Integration`
  - Risk class: `maintainability / data integrity`
  - Property/matrix focus: `artifact classification matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `naming / migration result / no accidental runtime path`
  - Test result: `NOT RUN`

- [ ] CL-MEM-03 文件與測試都描述同一條 canonical path
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `UT-restart-semantics-documented`
  - Test level: `Review / Unit`
  - Risk class: `diagnosability / regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / deprecation clarity`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for canonical provider wiring and deprecated path guards
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：此變更是 owner 收斂與 migration 策略）
- [ ] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason
- [ ] Adversarial/penetration-style cases are added/updated for wrong provider wiring / misleading docs / stale schema artifacts
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only「class exists」
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: canonical wiring
- Requirement mapping: `R1.x / CL-MEM-01`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `unit wiring tests`
- Reason: 核心風險在 module wiring，不需要 E2E

### Decision Record 2: legacy artifact cleanup
- Requirement mapping: `R2.x-R3.x / CL-MEM-02..03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-migration-audit-repurpose-or-drop`
- Reason: schema/drop/rename 需要 migration 級別驗證

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `N/A`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: canonical owner 收斂
- Requirement mapping: `R1.x / Task 1 / CL-MEM-01`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: legacy/audit path 整理
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-MEM-02..03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
