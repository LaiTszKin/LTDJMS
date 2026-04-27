# Design: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating
- Change Name: ai-agent-channel-gating

## Design Goal
把 mention 入口從「單一 allowlist gate」重構成「先判斷頻道模式，再套用對應授權規則」的可測決策模型。

## Change Summary
- Requested change: 修正 AI Agent 頻道仍依賴一般 AI 白名單的架構耦合
- Existing baseline: `AIChatMentionListener` 在讀取 `agentEnabled` 前先做 `channelRestrictionService.isChannelAllowed(...)` 檢查，未放行時直接 return
- Proposed design delta: 將 mention gating 抽成顯式 routing decision，Agent 與 AI Chat 各自有獨立的 entry condition，並在文件 / 測試 / panel 語意上對齊

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `aichat/commands`, `aiagent/services`, `panel/services`, 相關測試與文件
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- `AIChatMentionListener` 先用 guild/channel/category allowlist 做早期過濾
- 若頻道未在 allowlist，listener 直接靜默忽略，不會進一步評估 Agent 啟用狀態
- admin panel 與文件則把 Agent 配置描述成獨立設定域，導致設定語意與 runtime path 分裂

## Proposed Architecture
- 在 mention 入口建立一個明確的 decision matrix：`AGENT_ROUTE`、`AI_CHAT_ROUTE`、`DENY`
- `AIChatMentionRoutingDecision` 負責讀取 agent 狀態與 allowlist 狀態，回傳帶 source 的 routing decision
- `agentEnabled` 成為 Agent 路徑的獨立 gate；只有在未啟用 Agent 時，才評估一般 AI Chat allowlist
- 若 agent config 無法讀取，routing 會 fail closed 為 `DENY`，避免誤放行到一般 AI Chat
- log / test fixtures 都以 decision matrix 為核心，而不是散落在多個 if-return 分支

## Component Changes

### Component 1: `AIChatMentionRoutingDecision`（名稱可調整）
- Responsibility: 根據 guild/channel/category、Agent 啟用狀態與一般 allowlist 狀態決定 mention 路徑
- Inputs: guildId、channelId、restrictionChannelId、categoryId、agentEnabled、allowlist status
- Outputs: routing decision + 可觀測原因碼
- Dependencies: `AIChannelRestrictionService`, `AIAgentChannelConfigService`
- Invariants: Agent-only channel 必須能放行；非 Agent 頻道不得因重構而放寬 AI Chat 權限

### Component 2: `AIChatMentionListener`
- Responsibility: 使用 routing decision 啟動 Agent 或 AI Chat 後續流程
- Inputs: Discord message event
- Outputs: Agent streaming response、一般 AI streaming response、或顯式 deny/log
- Dependencies: routing decision abstraction、既有 streaming handlers
- Invariants: DM / bot / no-mention ignore 邏輯維持不變；下游串流處理不感知 gating 差異

## Sequence / Control Flow
1. listener 先過濾 bot / DM / no-mention 等共同前置條件
2. listener 讀取 Agent 啟用狀態與一般 allowlist 狀態，交由 routing decision 決定路徑
3. 若為 `AGENT_ROUTE`，直接進入 Agent 工具鏈；若為 `AI_CHAT_ROUTE`，走一般 AI Chat；否則顯式 deny/log
4. 測試與文件以同一 decision matrix 驗證/說明

## Data / State Impact
- Created or updated data: 無新增持久化資料；僅更新 routing 邏輯、測試與文件
- Consistency rules: `panel`、文件、單元測試與 runtime 必須使用同一條 canonical gating 規則
- Migration / rollout needs: None

## Risk and Tradeoffs
- Key risks: thread/category 解析回歸、雙重啟用時的優先順序不一致、維運誤讀 deny 原因
- Rejected alternatives:
  - 只改文件不改 runtime：無法修正實際行為
  - 讓 Agent 也強制遵守 AI 白名單：與既有產品語意衝突，且無法解決 issue
- Operational constraints: 若增加 logging，需避免在高頻 mention channel 產生過量噪音

## Validation Plan
- Tests: `UT` routing matrix、listener regression tests、panel facade smoke coverage
- Contract checks: `N/A`
- Rollback / fallback: 若實作出現問題，可直接回退 listener routing helper 與 log 調整；此次變更未引入 feature flag

## Open Questions
None
