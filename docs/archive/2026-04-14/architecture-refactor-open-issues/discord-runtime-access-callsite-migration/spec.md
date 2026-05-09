# Spec: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration
- Owner: Codex

## Goal
將 `aiagent`、`aichat`、`shop` 內直接依賴 `JDAProvider.getJda()` 的程式遷移到正式注入的 Discord runtime gateway，並移除這些模組對 process-global 測試狀態的依賴。

## Scope

### In Scope
- 遷移 `aiagent`、`aichat`、`shop` 內的 direct static JDA access call sites
- 改造這些模組的 constructor injection、helper abstraction 與測試 fixture
- 補齊禁止新增 direct static dependency 的 guardrails

### Out of Scope
- 設計 core runtime gateway 本身（由 `discord-runtime-access-core` 處理）
- 遷移 repo 內所有剩餘模組到新 gateway
- 調整 Discord 功能本身的業務規則

## Functional Behaviors (BDD)

### Requirement 1: Owned modules 不可再直接讀取 `JDAProvider`
**GIVEN** `aiagent`、`aichat`、`shop` 的服務/監聽器需要 Discord runtime  
**WHEN** 本次遷移完成  
**THEN** 這些模組必須透過正式注入的 gateway 或更窄 adapter 取得所需能力  
**AND** 不可再保留 `JDAProvider.getJda()` 直呼為主要路徑

**Requirements**:
- [x] R1.1 `aiagent` owned call sites 全部改為注入 gateway/adapter
- [x] R1.2 `aichat` owned call sites 全部改為注入 gateway/adapter
- [x] R1.3 `shop` owned call sites 全部改為注入 gateway/adapter

證據：`aiagent`、`aichat`、`shop` 的服務與測試都已改為注入式 `DiscordRuntimeGateway` / fake gateway，`mvn test` 通過，且 direct static `JDAProvider.getJda()` guardrail 維持綠燈。

### Requirement 2: 測試必須擺脫 process-global mutable state
**GIVEN** 目前多個單元測試需要 `JDAProvider.setJda()` / `clear()` 才能執行  
**WHEN** call site migration 完成  
**THEN** 這些測試應改用 module-local fake / mock gateway  
**AND** 不可再因共用 global singleton 導致測試互相污染

**Requirements**:
- [x] R2.1 `aiagent`、`aichat`、`shop` 測試必須可用 fake gateway 單獨執行
- [x] R2.2 測試 fixture 必須能表達 not-ready、guild not found、DM failure 等情境
- [x] R2.3 不可再需要在每個測試 class 前後手動清空 global singleton

證據：測試 fixture 已從 `JDAProvider.setJda()/clear()` 轉為 module-local mock/fake gateway；完整 `mvn test` 成功，且 `guild not found`、not-ready、DM failure 等分支都可在單一測試內獨立模擬。

### Requirement 3: 遷移後的模組邊界必須更容易維護與擴充
**GIVEN** runtime access 已被 core spec 收斂到正式 gateway  
**WHEN** owned modules 完成遷移  
**THEN** 新功能必須能沿用相同 injection boundary 擴充  
**AND** repo 必須有 guardrail 防止 direct static call site 重新擴散

**Requirements**:
- [x] R3.1 各模組的 helper / service 應依其實際需求注入窄介面，而不是整個 JDA
- [x] R3.2 至少要有一個靜態搜尋/測試 guardrail，防止新增 `JDAProvider.getJda()` call site
- [x] R3.3 文件與測試 helper 必須指向新遷移模式，而不是舊 singleton setup

證據：`NoDirectJdaProviderCallsitesTest` 已固定 owned modules 的 direct static callsite 邊界，相關文件與測試 helper 也已切換到 injected gateway 模式。

## Error and Edge Cases
- [x] guild 不存在、DM 開啟失敗、thread 取歷史失敗時，adapter 必須保留既有業務錯誤語意
- [x] not-ready runtime path 必須可被單元測試獨立模擬
- [x] 若某些工具只需 bot user id，不應被迫注入過寬的 Discord runtime 物件
- [x] 部分模組先遷移、部分模組仍在 bridge 上時，不得互相混淆 owner
- [x] guardrail 不得誤傷 bootstrap/core bridge 合法用法

證據：相關錯誤情境已在 fake gateway / mock fixture 下可重現並維持原本錯誤語意，且 bootstrap/core bridge 仍未被 guardrail 誤判。

## Clarification Questions
None

## References
- Official docs:
  - https://docs.jda.wiki/net/dv8tion/jda/api/JDA.html
- Related code files:
  - `src/main/java/ltdjms/discord/aiagent/services/DefaultAIAgentChannelConfigService.java`
  - `src/main/java/ltdjms/discord/aiagent/services/DiscordThreadHistoryProvider.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderBuyerNotificationService.java`
  - `src/main/java/ltdjms/discord/shop/services/ShopAdminNotificationService.java`
  - `src/main/java/ltdjms/discord/aichat/services/LangChain4jAIChatService.java`
