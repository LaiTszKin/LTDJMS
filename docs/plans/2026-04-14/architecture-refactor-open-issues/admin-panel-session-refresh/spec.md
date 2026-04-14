# Spec: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh
- Owner: Codex

## Goal
讓 `/admin-panel` 的 guild-wide 更新真正透過 session abstraction 刷新開啟中的面板，而不是在事件管線中記錄 warning 後靜默失效。

## Scope

### In Scope
- 擴充 admin panel session abstraction，使其支援按 guild 枚舉有效 session
- 收斂一般 admin panel 與 product 子頁面的刷新路徑
- 對齊事件 listener、session metadata、測試與文件

### Out of Scope
- 重做整個 admin panel UI 導覽或頁面內容
- 改寫 DomainEventPublisher 同步分發模型
- 建立跨進程 session store 或 Redis 分散式 session 系統

## Functional Behaviors (BDD)

### Requirement 1: Guild-wide session traversal 必須正式被 session abstraction 支援
**GIVEN** 某個 guild 中有一個或多個管理員保持 `/admin-panel` 開啟  
**WHEN** 事件 listener 需要刷新該 guild 的 active admin panels  
**THEN** session manager 必須能枚舉該 guild 的有效 session  
**AND** 不可再把 `updatePanelsByGuild(...)` 留在未實作狀態

**Requirements**:
- [ ] R1.1 `AdminPanelSessionManager` 或其底層抽象必須支援按 guild 取得有效 session 集合
- [ ] R1.2 過期或失效 hook 必須在 guild-wide traversal 中被安全清理
- [ ] R1.3 session metadata 必須足以支援刷新當前 view，而不是只知道 guildId/adminId

### Requirement 2: Event-driven refresh 必須覆蓋主要 admin panel 與子頁
**GIVEN** 貨幣、骰子、商品或兌換碼事件發生  
**WHEN** `AdminPanelUpdateListener` 收到事件  
**THEN** 開啟中的 admin panel 必須根據各自 view context 進行正確刷新  
**AND** 不可再以 product side map 或特例邏輯補洞作為主要刷新機制

**Requirements**:
- [ ] R2.1 `AdminPanelUpdateListener` 必須透過正式的 session traversal / view refresh path 更新主要頁面
- [ ] R2.2 product 子頁刷新若仍需特別上下文，也必須落入統一 session metadata 模型
- [ ] R2.3 事件刷新不可破壞原本的 ephemeral hook 生命週期與錯誤隔離行為

### Requirement 3: 文件與測試必須反映真實 refresh 能力
**GIVEN** 目前文件與測試已把 admin panel 描述成 event-driven real-time update  
**WHEN** 此次重構完成  
**THEN** 文件與測試都必須對齊真正可支援的刷新範圍與 session 條件  
**AND** 不可再讓文件宣稱有即時更新但 runtime 實際只是 no-op

**Requirements**:
- [ ] R3.1 單元測試必須覆蓋 guild-wide traversal、expired session cleanup 與多 view refresh
- [ ] R3.2 文件必須說明 admin panel session 保存哪些 view context
- [ ] R3.3 若有仍無法即時刷新的 view，必須在 spec/實作中明確列出而不是隱性忽略

## Error and Edge Cases
- [ ] 同一 guild 同時有多個管理員開啟不同子頁時，刷新不可互相覆蓋錯誤 view
- [ ] hook 過期或 Discord 編輯失敗時，session 必須被安全移除，不得讓 traversal 永遠保留髒資料
- [ ] 高頻事件下不可導致對同一 hook 重複編輯風暴
- [ ] product 子頁與主頁若共享 session key，需避免 side map 與主索引漂移
- [ ] 若底層 `DiscordSessionManager` 不適合支援枚舉，必須重新界定 abstraction，而不是再加一層 ad-hoc map

## Clarification Questions
None

## References
- Official docs:
  - None（此規格聚焦 repo 內部 session abstraction 與事件刷新）
- Related code files:
  - `src/main/java/ltdjms/discord/panel/services/AdminPanelSessionManager.java`
  - `src/main/java/ltdjms/discord/panel/services/AdminPanelUpdateListener.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminProductPanelHandler.java`
  - `src/test/java/ltdjms/discord/panel/services/AdminPanelUpdateListenerTest.java`
