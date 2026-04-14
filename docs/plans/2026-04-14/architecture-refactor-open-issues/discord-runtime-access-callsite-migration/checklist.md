# Checklist: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：issue 與 call site 證據已足夠）
- [ ] Affected plans are reviewed/updated（approval 後若 adapter 切分調整，需同步 core spec 與 coordination）
- [ ] Explicit user approval on updated specs is obtained（date/conversation reference: 待使用者批准）

## Behavior-to-Test Checklist
- [ ] CL-CALLSITE-01 `aiagent`/`aichat`/`shop` 不再直接呼叫 `JDAProvider.getJda()`
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-no-direct-jdaprovider-aiagent`, `UT-no-direct-jdaprovider-aichat`, `UT-no-direct-jdaprovider-shop`
  - Test level: `Unit / Guardrail`
  - Risk class: `maintainability / regression`
  - Property/matrix focus: `boundary invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `owned modules zero direct static callsite`
  - Test result: `NOT RUN`

- [ ] CL-CALLSITE-02 測試可用 fake gateway 獨立執行
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-fake-runtime-gateway`, `UT-shop-dm-failure-via-fake`, `UT-thread-history-not-ready`
  - Test level: `Unit`
  - Risk class: `test isolation / regression`
  - Property/matrix focus: `error-state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `no global mutable state / preserved error semantics`
  - Test result: `NOT RUN`

- [ ] CL-CALLSITE-03 guardrail 能阻止 direct static dependency 回流
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `CI-search-jdaprovider-owned-modules`, `DOC-review`
  - Test level: `Guardrail / Review`
  - Risk class: `maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `search rule / updated developer guidance`
  - Test result: `NOT RUN`

## Required Hardening Records
- [ ] Regression tests are added/updated for migrated runtime-dependent services
- [ ] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（預期 `N/A`：此次為 dependency boundary 遷移）
- [ ] External services in the business logic chain are mocked/faked for scenario testing
- [ ] Adversarial/penetration-style cases are added/updated for not-ready、guild not found、DM failure、thread history failure
- [ ] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [ ] Assertions verify business outcomes and side effects/no-side-effects, not only「compiles」
- [ ] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: migrated adapters preserve behavior
- Requirement mapping: `R1.x-R2.x / CL-CALLSITE-01..02`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-shop-notification-via-gateway`, `IT-thread-history-via-gateway`
- Reason: 需跨 service + adapter 驗證，但不需要真 Discord E2E

### Decision Record 2: guardrail against regression
- Requirement mapping: `R3.x / CL-CALLSITE-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `CI-search-jdaprovider-owned-modules`
- Reason: 靜態搜尋/測試 guardrail 已足夠擋回流

## Execution Summary
- [ ] Unit tests: `NOT RUN`
- [ ] Regression tests: `NOT RUN`
- [ ] Property-based tests: `N/A`
- [ ] Integration tests: `NOT RUN`
- [ ] E2E tests: `N/A`
- [ ] External service mock scenarios: `NOT RUN`
- [ ] Adversarial/penetration-style cases: `NOT RUN`

## Completion Records

### Completion Record 1: owned modules call site migration
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-CALLSITE-01..02`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `等待 approval`

### Completion Record 2: guardrail 與文件
- Requirement mapping: `R3.x / Task 3 / CL-CALLSITE-03`
- Completion status: `deferred`
- Remaining applicable items: `全部`
- Notes: `尚未實作`
