# Design: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh
- Change Name: admin-panel-session-refresh

## Design Goal
讓 admin panel 的 event-driven 更新路徑真正有可枚舉 session 與 view context 可用，而不是把 refresh 需求推遲到「下次互動再說」。

## Change Summary
- Requested change: 修正 `AdminPanelUpdateListener` 依賴未實作 guild-wide refresh 的問題
- Existing baseline: `AdminPanelSessionManager` 只能 point lookup，無 guild-wide traversal；product 子頁另外維護 side map 補洞；listener 只記錄 warning
- Proposed design delta: 在 session abstraction 中正式保存可枚舉索引與 view metadata，讓 listener 透過 main/product refresh path 對 active admin panels 做真實刷新

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `panel/services`, `panel/commands`, `discord/domain|services`, tests/docs
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- `AdminPanelSessionManager` 以泛型 `InteractionSessionManager` 保存 hook，並以本地 view 索引保存 `MAIN` / `PRODUCT_LIST` / `PRODUCT_DETAIL` / `PRODUCT_CODE_LIST`
- `AdminPanelUpdateListener` 依事件型別分流到 `AdminPanelButtonHandler.refreshMainPanels(...)` 或 `AdminProductPanelHandler.refreshProductPanels(...)`
- product 子頁不再依賴 ad-hoc `productSessions` 作為主要刷新來源；refresh state 由 session metadata 提供

## Proposed Architecture
- 讓 panel session abstraction 顯式保存：guildId、adminId、current view、必要的 product/page context、hook reference
- 底層支援按 guild 枚舉有效 session，並在遍歷時清理失效 hook 或失敗的 edit path
- `AdminPanelUpdateListener` 針對不同事件類型選擇 main panel refresh 或 product panel refresh，避免只記錄「有變動」

## Component Changes

### Component 1: `AdminPanelSessionManager`
- Responsibility: 註冊、枚舉、更新與清理 admin panel session
- Inputs: guildId、adminId、hook、view metadata
- Outputs: point lookup、guild traversal、safe cleanup
- Dependencies: `DiscordSessionManager` + 本地 session index
- Invariants: session metadata 與當前 view 同步；失效 hook 會被移除；guild-wide traversal 不會洩漏其他 guild 的 session

### Component 2: `AdminPanelUpdateListener`
- Responsibility: 將 domain events 映射為特定 view refresh 行為
- Inputs: currency/game/product/redemption events
- Outputs: 對 active sessions 的 embed / component refresh
- Dependencies: `AdminPanelButtonHandler`, `AdminProductPanelHandler`
- Invariants: listener failure 不得中斷其他 event listeners；不同 view 的 refresh 不互相污染；非支援 view 保持手動導覽

## Sequence / Control Flow
1. `/admin-panel` 開啟或切換 view 時註冊/更新 session metadata
2. domain event 發生後，`AdminPanelUpdateListener` 先決定應刷新 main panel 或 product panel
3. handler 透過 `AdminPanelSessionManager.updatePanelsByGuild(...)` 按 guild 枚舉有效 session
4. handler 依 session current view 與 metadata 建構對應 refresh payload
5. hook 編輯成功則維持 session；失敗或 metadata 已失效則移除 session
6. 文件與測試對齊新的 session capabilities 與明確的 manual views

## Data / State Impact
- Created or updated data: in-memory session metadata / guild index；無持久化 schema 變更
- Consistency rules: 任何 ad-hoc side map 都要麼併入 canonical session metadata，要麼被移除
- Migration / rollout needs: None

## Risk and Tradeoffs
- Key risks: view metadata 設計過窄導致仍需特例、hook edit race、遍歷成本在高活躍 guild 下增加、部分 transient admin subviews 仍需重新導覽
- Rejected alternatives:
  - 保持現狀只修文件：無法滿足 runtime 行為
  - 繼續為不同子頁維護 side map：會複製同一類 session owner 問題
- Operational constraints: refresh builder 不可做過重的同步查詢，避免拖慢事件分發

## Validation Plan
- Tests: `UT` session registration/traversal、多 view refresh matrix、失效 hook cleanup、listener routing
- Contract checks: `N/A`
- Rollback / fallback: 若 traversal abstraction 需回退，可先保留新 metadata 但停用自動刷新；不得再把未實作 method 當正式能力

## Open Questions
None
