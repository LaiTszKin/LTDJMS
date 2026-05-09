# Checklist: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating

## Clarification & Approval Gate
- [ ] User clarification responses are recorded（N/A：目前 issue 與 repo 證據已足以定義需求）
- [x] Affected plans are reviewed/updated（已同步更新 spec、tasks、checklist、design 與對應文件）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 2026-04-27，本次 implement-specs-with-worktree 指令即為批准）

## Behavior-to-Test Checklist
- [x] CL-GATE-01 Agent-only channel mention 會走 Agent 路徑
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `AIChatMentionRoutingDecisionTest.shouldPreferAgentRouteWhenAgentEnabled`, `AIChatMentionListenerTest.ChannelCheck.shouldTriggerAgentResponseWhenAgentEnabledEvenIfAllowlistDenied`, `AIChatMentionListenerAgentConclusionTest.shouldSendToolIntentBeforeFinalConclusionInAgentMode`
  - Test level: `Unit`
  - Risk class: `authorization / regression`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `decision result / side effects / no silent drop`
  - Test result: `PASS`

- [x] CL-GATE-02 Non-agent channel 仍遵守 AI allowlist
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `AIChatMentionRoutingDecisionTest.shouldUseAllowlistWhenAgentDisabled`, `AIChatMentionListenerTest.ChannelCheck.shouldNotTriggerAIResponseWhenChannelNotAllowed`, `AIChatMentionListenerTest.ChannelCheck.shouldUseParentChannelForRestrictionWhenThread`
  - Test level: `Unit`
  - Risk class: `authorization / regression`
  - Property/matrix focus: `decision matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `permission denial / unchanged fallback behavior`
  - Test result: `PASS`

- [x] CL-GATE-03 文件、panel 與測試都以相同 routing matrix 表達行為
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `AIConfigManagementFacadeTest`, `docs/features.md`, `docs/architecture.md`, `docs/modules/aichat.md`, `docs/modules/aiagent.md`, `docs/modules/panels.md`
  - Test level: `Unit / Review`
  - Risk class: `regression / diagnosability`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `documentation parity / test parity`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for mention gating high-risk behavior
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（`N/A`：decision matrix 有限且離散，表格化單元測試比 PBT 更可讀）
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason（`N/A`：無外部服務鏈）
- [x] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons（authorization / invalid transition: covered；replay/idempotency / concurrency: `N/A`，此 mention gating 為 read-only stateless path）
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only「does not throw」
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason

## E2E / Integration Decision Records

### Decision Record 1: mention routing matrix
- Requirement mapping: `R1.x-R2.x / CL-GATE-01..02`
- Decision: `Covered with targeted unit tests`
- Linked case IDs: `AIChatMentionRoutingDecisionTest`, `AIChatMentionListenerTest`
- Reason: 風險集中在 listener 與 routing helper wiring；unit test 已足夠驗證 route/source 與 side effects

### Decision Record 2: panel / docs parity
- Requirement mapping: `R3.x / CL-GATE-03`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `doc review + targeted unit tests`
- Reason: 風險在語意一致性，不需要額外 E2E

## Execution Summary
- [x] Unit tests: `PASS - mvn test -Dtest=AIChatMentionRoutingDecisionTest,AIChatMentionListenerTest,AIChatMentionListenerAgentConclusionTest,AIConfigManagementFacadeTest`
- [x] Regression tests: `PASS - same command as above`
- [x] Property-based tests: `N/A - decision matrix is finite and discrete; table-driven unit coverage is clearer`
- [x] Integration tests: `N/A - listener + routing helper unit coverage already exercises the routed chain`
- [x] E2E tests: `N/A - Discord live interaction is unnecessary for this routing boundary`
- [x] External service mock scenarios: `N/A - no external service chain; internal services are mocked`
- [x] Adversarial/penetration-style cases: `PASS - agent config unavailable falls back to deny`

## Completion Records

### Completion Record 1: mention routing 重構
- Requirement mapping: `R1.x-R2.x / Task 1 / CL-GATE-01..02`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `AIChatMentionRoutingDecision` 已落地，listener 先做 agent gate 再做 allowlist，並補上 fail-closed 測試`

### Completion Record 2: 文件與測試對齊
- Requirement mapping: `R3.x / Task 2-3 / CL-GATE-03`
- Completion status: `completed`
- Remaining applicable items: `none`
- Notes: `panel` 語意、文件與測試矩陣已同步為同一 routing matrix`
