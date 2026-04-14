# Design: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh
- Change Name: admin-panel-session-refresh

## Design Goal
讓 admin panel 的 event-driven 更新路徑真正有可枚舉 session 與 view context 可用，而不是把 refresh 需求推遲到「下次互動再說」。

## Change Summary
- Requested change: 修正 `AdminPanelUpdateListener` 依賴未實作 `updatePanelsByGuild(...)` 的問題
- Existing baseline: `AdminPanelSessionManager` 只能 point lookup，無 guild-wide traversal；product 子頁另外維護 side map 補洞
- Proposed design delta: 在 session abstraction 中正式保存可枚舉索引與 view metadata，讓 listener 能對 active admin panels 做真實刷新

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `panel/services`, `panel/commands`, `discord/domain|services`, tests/docs
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- `AdminPanelSessionManager` 使用泛型 `InteractionSessionManager`，只支援以 guildId + adminId 取回單一 session
- `AdminPanelUpdateListener` 仍呼叫未實作的 guild-wide update method
- product 子頁為了補足局部刷新，另存 `productSessions` 類型的 ad-hoc 索引

## Proposed Architecture
- 讓 panel session abstraction 顯式保存：guildId、adminId、current view、必要的 product/page context、hook reference
- 底層支援按 guild 枚舉有效 session，並在遍歷時清理失效 hook
- `AdminPanelUpdateListener` 針對不同事件類型選擇對應 view refresh builder，而不是只記錄「有變動」

## Component Changes

### Component 1: `AdminPanelSessionManager`
- Responsibility: 註冊、枚舉、更新與清理 admin panel session
- Inputs: guildId、adminId、hook、view metadata
- Outputs: point lookup、guild traversal、safe cleanup
- Dependencies: `DiscordSessionManager` 或其替代抽象
- Invariants: session metadata 與當前 view 同步；失效 hook 會被移除

### Component 2: `AdminPanelUpdateListener`
- Responsibility: 將 domain events 映射為特定 view refresh 行為
- Inputs: currency/game/product/redemption events
- Outputs: 對 active sessions 的 embed / component refresh
- Dependencies: `AdminPanelSessionManager`, panel view builders/handlers
- Invariants: listener failure 不得中斷其他 event listeners；不同 view 的 refresh 不互相污染

## Sequence / Control Flow
1. `/admin-panel` 開啟或切換 view 時註冊/更新 session metadata
2. domain event 發生後，`AdminPanelUpdateListener` 按 guild 枚舉有效 session
3. listener 依 session current view 建構對應 refresh payload
4. hook 編輯成功則維持 session；失敗則移除失效 session
5. 文件與測試對齊新的 session capabilities

## Data / State Impact
- Created or updated data: in-memory session metadata / guild index；無持久化 schema 變更
- Consistency rules: 任何 ad-hoc side map 都要麼併入 canonical session metadata，要麼被移除
- Migration / rollout needs: None

## Risk and Tradeoffs
- Key risks: view metadata 設計過窄導致仍需特例、hook edit race、遍歷成本在高活躍 guild 下增加
- Rejected alternatives:
  - 保持現狀只修文件：無法滿足 runtime 行為
  - 繼續為不同子頁維護 side map：會複製同一類 session owner 問題
- Operational constraints: refresh builder 不可做過重的同步查詢，避免拖慢事件分發

## Validation Plan
- Tests: `UT` session registration/traversal、多 view refresh matrix、失效 hook cleanup
- Contract checks: `N/A`
- Rollback / fallback: 若 traversal abstraction 需回退，可先保留新 metadata 但停用自動刷新；不得再把未實作 method 當正式能力

## Open Questions
None
