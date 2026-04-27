# Checklist: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source

## Clarification & Approval Gate
- [x] User clarification responses are recorded（N/A：本次沒有額外澄清需求）
- [x] Affected plans are reviewed/updated（已同步 spec / tasks / checklist / contract / design / docs）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 2026-04-27 本次請求授權實作）

## Behavior-to-Test Checklist
- [x] CL-MEM-01 只有單一 runtime ChatMemoryProvider 是 canonical owner
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-chat-memory-canonical-path`, `UT-module-wiring-memory-provider`
  - Test level: `Unit`
  - Risk class: `regression / maintainability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `single wiring path / no shadow provider`
  - Test result: `PASS` (`mvn test -Dtest=AIAgentModuleTest`, `mvn test -Dtest=SimplifiedChatMemoryProviderTest`)

- [x] CL-MEM-02 runtime memory 與 audit/legacy artifacts 分責清楚
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-legacy-provider-not-runtime`, `IT-migration-audit-repurpose-or-drop`
  - Test level: `Unit / Integration`
  - Risk class: `maintainability / data integrity`
  - Property/matrix focus: `artifact classification matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `naming / migration result / no accidental runtime path`
  - Test result: `PASS` (`mvn test -Dtest=PersistentChatMemoryProviderTest`, `mvn test -Dtest=RedisPostgresChatMemoryStoreTest`; integration case `IT-migration-audit-repurpose-or-drop` recorded as `N/A` because this batch only repurposed comments/docs, not live migration logic)

- [x] CL-MEM-03 文件與測試都描述同一條 canonical path
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `DOC-review`, `UT-restart-semantics-documented`
  - Test level: `Review / Unit`
  - Risk class: `diagnosability / regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / deprecation clarity`
  - Test result: `PASS`（docs/modules/aiagent.md、docs/modules/aichat.md、V012 comments updated）

## Required Hardening Records
- [x] Regression tests are added/updated for canonical provider wiring and deprecated path guards
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：此變更只涉及 owner 收斂、命名與 migration / docs backfill，沒有可抽樣的商業輸入空間）
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason（N/A：canonical runtime path 不依賴外部服務）
- [x] Adversarial/penetration-style cases are added/updated for wrong provider wiring / misleading docs / stale schema artifacts
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons（N/A：本次變更不改變授權、狀態轉移或 idempotency 邏輯）
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only「class exists」
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

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
- [x] Unit tests: `PASS` (`AIAgentModuleTest`, `SimplifiedChatMemoryProviderTest`, `LangChain4jAIChatServiceTest`, `PersistentChatMemoryProviderTest`, `RedisPostgresChatMemoryStoreTest`)
- [x] Regression tests: `PASS`（canonical wiring + deprecated guard rails）
- [x] Property-based tests: `N/A`（owner 收斂 / naming / docs backfill 沒有 business-rule input space）
- [x] Integration tests: `N/A`（this batch only required unit-level regression and docs/migration comment backfill; no live integration surface changed）
- [x] E2E tests: `N/A`（沒有使用者流程變更）
- [x] External service mock scenarios: `N/A`（canonical path 無外部服務鏈）
- [x] Adversarial/penetration-style cases: `PASS`（wrong provider wiring / misleading docs / stale schema artifacts）

## Completion Records

### Completion Record 1: canonical owner 收斂
- Requirement mapping: `R1.x / Task 1 / CL-MEM-01`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `AIAgentModule 直接接線 SimplifiedChatMemoryProvider；module test 已覆蓋`

### Completion Record 2: legacy/audit path 整理
- Requirement mapping: `R2.x-R3.x / Task 2-3 / CL-MEM-02..03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `legacy provider / repository / migration / docs 已明確標記非 canonical`
