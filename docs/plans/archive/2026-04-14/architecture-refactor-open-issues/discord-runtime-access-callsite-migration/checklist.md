# Checklist: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：issue 與 call site 證據已足夠）
- [x] Affected plans are reviewed/updated（approval 後若 adapter 切分調整，需同步 core spec 與 coordination）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 本次 continue 指示已延續既有 spec）

## Behavior-to-Test Checklist
- [x] CL-CALLSITE-01 `aiagent`/`aichat`/`shop` 不再直接呼叫 `JDAProvider.getJda()`
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-no-direct-jdaprovider-aiagent`, `UT-no-direct-jdaprovider-aichat`, `UT-no-direct-jdaprovider-shop`
  - Test level: `Unit / Guardrail`
  - Risk class: `maintainability / regression`
  - Property/matrix focus: `boundary invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `owned modules zero direct static callsite`
  - Test result: `PASS (guardrail test + full mvn test)`

- [x] CL-CALLSITE-02 測試可用 fake gateway 獨立執行
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-fake-runtime-gateway`, `UT-shop-dm-failure-via-fake`, `UT-thread-history-not-ready`
  - Test level: `Unit`
  - Risk class: `test isolation / regression`
  - Property/matrix focus: `error-state matrix`
  - External dependency strategy: `mocked service states`
  - Oracle/assertion focus: `no global mutable state / preserved error semantics`
  - Test result: `PASS (module-local mock/fake gateway)`

- [x] CL-CALLSITE-03 guardrail 能阻止 direct static dependency 回流
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `CI-search-jdaprovider-owned-modules`, `DOC-review`
  - Test level: `Guardrail / Review`
  - Risk class: `maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `search rule / updated developer guidance`
  - Test result: `PASS (static search guardrail + full mvn test)`

## Required Hardening Records
- [x] Regression tests are added/updated for migrated runtime-dependent services
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：此次為 dependency boundary 遷移，未改變輸入值域或狀態轉移規則）
- [x] External services in the business logic chain are mocked/faked for scenario testing
- [x] Adversarial/penetration-style cases are added/updated for not-ready、guild not found、DM failure、thread history failure
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only「compiles」
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `PASS (targeted verification + full mvn test)`
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`
- [x] Integration tests: `PASS (full mvn test)`
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `PASS`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: owned modules call site migration
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-CALLSITE-01..02`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `aiagent`、`aichat`、`shop` 已完成 gateway migration，guardrail 與測試隔離已到位，`mvn test` 成功。

### Completion Record 2: guardrail 與文件
- Requirement mapping: `R3.x / Task 3 / CL-CALLSITE-03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: guardrail、文件與 fake gateway 測試 helper 已同步完成。
