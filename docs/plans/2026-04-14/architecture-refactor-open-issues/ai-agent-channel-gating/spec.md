# Spec: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating
- Owner: Codex

## Goal
讓 AI Agent 頻道啟用狀態真正成為獨立的 mention 入口邊界，使維運人員可以建立「只允許 Agent、禁止一般 AI Chat」的頻道。

## Scope

### In Scope
- 重構 `AIChatMentionListener` 的入口判斷順序，讓 Agent 與一般 AI Chat 使用明確的 routing decision
- 對齊 `aichat`、`aiagent`、`panel` 三處設定語意與測試
- 更新文件與行為說明，明確區分 Agent 啟用清單與 AI 白名單

### Out of Scope
- 改寫 AI Chat / Agent 的 prompt、模型、工具內容
- 新增第三套頻道權限模型或角色級 Agent 授權系統
- 變更 slash command / admin panel 的整體 UI 架構

## Functional Behaviors (BDD)

### Requirement 1: Agent 頻道必須優先走 Agent routing
**GIVEN** 某個 guild 頻道已被啟用為 AI Agent 頻道  
**AND** 使用者在該頻道提及 Bot  
**WHEN** mention 入口評估該訊息  
**THEN** 系統必須在一般 AI 白名單檢查之前或獨立於其之外決定走 Agent 工具鏈  
**AND** 不可因該頻道未加入一般 AI 白名單而直接靜默忽略

**Requirements**:
- [ ] R1.1 mention routing 必須先判定 `agentEnabled`，並把 Agent 路徑視為獨立可用條件
- [ ] R1.2 Agent 啟用為 `true` 時，即使 `AIChannelRestrictionService` 回傳 `false`，仍必須允許 Agent 路徑繼續
- [ ] R1.3 日誌與除錯訊息必須能明確區分「Agent 放行」與「AI Chat 白名單放行」兩種決策來源

### Requirement 2: 一般 AI Chat 白名單語意必須維持不變
**GIVEN** 某個頻道未啟用 AI Agent  
**AND** 使用者在該頻道提及 Bot  
**WHEN** mention 入口評估該訊息  
**THEN** 系統仍必須以既有 AI 白名單 / 類別白名單作為一般 AI Chat 的放行條件  
**AND** 不得因 Agent routing 重構而放寬原本的 AI Chat 授權邊界

**Requirements**:
- [ ] R2.1 非 Agent 頻道仍必須遵守現有 `AIChannelRestrictionService` allowlist 規則
- [ ] R2.2 DM、bot 訊息、未 mention 訊息的忽略行為必須維持不變
- [ ] R2.3 category allowlist 與 channel allowlist 的解析規則不得因 routing 重構而改變

### Requirement 3: 設定面與測試必須反映新的 canonical 行為
**GIVEN** admin panel、文件與測試套件都宣告 AI Agent 與一般 AI Chat 是兩套設定域  
**WHEN** 本次重構完成  
**THEN** 程式、文件與測試都必須以相同 decision matrix 描述 mention routing  
**AND** 不可再保留要求「Agent 啟用且白名單也必須放行」的舊測試前提

**Requirements**:
- [ ] R3.1 `panel` 相關說明與 facade 語意必須明確標示 Agent 頻道可獨立於 AI 白名單啟用
- [ ] R3.2 單元測試必須新增 Agent-only channel case，並保留 non-agent allowlist regression coverage
- [ ] R3.3 文件中的流程圖與 feature 說明必須對齊新 routing 規則

## Error and Edge Cases
- [ ] Agent 啟用但 `agentConfigService` 暫時不可用時，必須有可觀測的 fallback / deny 行為，而不是誤放行
- [ ] category allowlist 命中但 channel 未啟用 Agent 時，不得誤走 Agent 路徑
- [ ] 同一頻道同時在 AI 白名單且 Agent 啟用時，必須有穩定且可文件化的 routing 優先順序
- [ ] 私有 thread / forum thread 的 restriction channel ID 解析不可被此次重構破壞
- [ ] 重複 mention、空訊息預設 greeting 與 reasoning / markdown streaming 行為不得被 gating 重構破壞

## Clarification Questions
None

## References
- Official docs:
  - None（此規格是 repo 內部 routing boundary 重構，沒有外部依賴決定行為）
- Related code files:
  - `src/main/java/ltdjms/discord/aichat/commands/AIChatMentionListener.java`
  - `src/main/java/ltdjms/discord/panel/services/AIConfigManagementFacade.java`
  - `src/test/java/ltdjms/discord/aichat/unit/commands/AIChatMentionListenerTest.java`
  - `src/test/java/ltdjms/discord/aichat/unit/commands/AIChatMentionListenerAgentConclusionTest.java`
