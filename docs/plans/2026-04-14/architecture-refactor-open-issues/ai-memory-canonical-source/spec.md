# Spec: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source
- Owner: Codex

## Goal
為 AI Agent 對話記憶指定單一 canonical owner，消除 runtime、資料表、deprecated provider 與文件之間的平行架構殘影。

## Scope

### In Scope
- 決定 AI Agent conversation memory 的 canonical runtime owner 與非 runtime 資料的定位
- 對齊 `AIAgentModule` wiring、provider/repository、文件與測試
- 明確規範 deprecated persistence 路徑是要移除、改名為 audit、或重新納入 runtime

### Out of Scope
- 更換 AI provider、模型或 LangChain4j 工具鏈
- 重新設計 Discord thread 歷史抓取演算法
- 建立新的長期觀察性資料平台

## Functional Behaviors (BDD)

### Requirement 1: Runtime conversation memory 必須有唯一 canonical owner
**GIVEN** AI Agent 執行時需要組合對話上下文  
**WHEN** `ChatMemoryProvider` 被建立與使用  
**THEN** 系統必須只有一條正式 runtime path 負責提供 conversation memory  
**AND** 其他資料表、provider 或 repository 若不在 runtime path 中，必須被明確標記為非 runtime 用途或移除

**Requirements**:
- [ ] R1.1 `AIAgentModule` 的 runtime wiring 必須與文件宣告的 canonical owner 完全一致
- [ ] R1.2 若選擇保留 `SimplifiedChatMemoryProvider` 為 canonical owner，舊 PostgreSQL / Redis path 必須改名或明示為 non-runtime artifact
- [ ] R1.3 若保留任何非 runtime conversation 資料表，必須定義清楚其用途、寫入者與查詢者

### Requirement 2: Runtime memory 與 audit/diagnostic 資料必須分責
**GIVEN** 系統可能仍需要保存工具調用或診斷資訊  
**WHEN** 維護者檢查資料表、provider 與測試  
**THEN** 必須能清楚分辨哪些資料會在 runtime 還原 conversation context、哪些只是診斷記錄  
**AND** 不可再讓未實作的 PostgreSQL save/delete 路徑被描述成可還原 runtime memory

**Requirements**:
- [ ] R2.1 runtime chat memory 與 diagnostic/audit storage 的命名、文件與測試必須分開
- [ ] R2.2 若工具調用歷史只存在記憶體，必須在文件中明示 restart 後的語義
- [ ] R2.3 若保留舊 migration 產生的 conversation tables，需定義 drop、rename 或 repurpose 策略

### Requirement 3: 文件、測試與 migration 必須反映最後選擇的 canonical 方向
**GIVEN** 本次重構是在收斂平行架構殘影  
**WHEN** 規格被實作  
**THEN** 文件、測試、deprecation 標記與 schema 必須一起收斂到同一方向  
**AND** 不可再留下第二套「看起來像 canonical、實際沒接線」的路徑

**Requirements**:
- [ ] R3.1 docs/modules/aiagent.md 與相關文件必須重寫成最終 canonical 設計
- [ ] R3.2 測試必須只保護選定的 canonical runtime path；被淘汰路徑只能保留 migration/drop/compat coverage
- [ ] R3.3 migration 與命名策略必須讓維護者一眼辨識哪些資料仍然有效、哪些是待淘汰 artifacts

## Error and Edge Cases
- [ ] bot 重啟後 thread history 可恢復但 in-memory tool call history 遺失時，語義必須被明示
- [ ] 若保留 conversation tables 供 audit 用途，不得再由 runtime 讀寫語意含糊的同名 provider
- [ ] 文件不可同時宣稱 Redis+PostgreSQL persistence 與 thread-only runtime memory 都是 canonical
- [ ] 若未實作的 provider 暫時保留，需避免被新功能誤接線
- [ ] 測試套件不得再要求維護兩套平行 runtime memory 假設

## Clarification Questions
None

## References
- Official docs:
  - https://docs.langchain4j.dev/tutorials/chat-memory/
  - https://docs.langchain4j.dev/apidocs/dev/langchain4j/memory/chat/ChatMemoryProvider.html
- Related code files:
  - `src/main/java/ltdjms/discord/shared/di/AIAgentModule.java`
  - `src/main/java/ltdjms/discord/aiagent/services/SimplifiedChatMemoryProvider.java`
  - `src/main/java/ltdjms/discord/aiagent/services/PersistentChatMemoryProvider.java`
  - `src/main/java/ltdjms/discord/aiagent/services/RedisPostgresChatMemoryStore.java`
  - `src/main/resources/db/migration/V012__agent_conversation_persistence.sql`
