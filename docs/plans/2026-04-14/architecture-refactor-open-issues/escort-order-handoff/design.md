# Design: Escort order handoff

- Date: 2026-04-14
- Feature: Escort order handoff
- Change Name: escort-order-handoff

## Design Goal
把商品/付款流程中的「需要護航」從一次性通知副作用升級成 dispatch 模組內可追蹤、可重試、可審計的正式工作項。

## Change Summary
- Requested change: 修正 `auto_create_escort_order` 商品在購買完成後沒有真正進入 dispatch aggregate 的問題
- Existing baseline: `shop` 只在購買完成後發送 admin DM，`dispatch` aggregate 沒有來源商品 / 訂單欄位可承接此脈絡
- Proposed design delta: 在 `shop` 與 `dispatch` 之間建立明確 handoff service，讓自動護航開單直接寫入 dispatch durable state，通知改為派生副作用

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `product`, `shop`, `dispatch`, 相關 migration / tests / docs
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- `Product` 保存 `autoCreateEscortOrder` 與 `escortOptionCode`
- 貨幣購買與法幣付款完成都會檢查商品設定，但目前只呼叫 `ShopAdminNotificationService`
- `EscortDispatchOrder` 缺少來源商品 / 訂單欄位，導致 purchase intent 無法跨模組落地

## Proposed Architecture
- 新增一個由 `shop` 觸發、`dispatch` 擁有的 handoff boundary（例如 `EscortDispatchHandoffService`）
- handoff payload 直接包含 source order reference、source type、source product snapshot、escort option snapshot、price snapshot
- `ShopAdminNotificationService` 改為讀取 dispatch record 產生通知，而不是直接從 product 拼湊一次性訊息

## Component Changes

### Component 1: `EscortDispatchHandoffService`（名稱可調整）
- Responsibility: 接收購買完成事件，冪等建立 dispatch work item
- Inputs: guildId、buyerUserId、source order info、product snapshot、escort option snapshot
- Outputs: 新建立或已存在的 dispatch order reference
- Dependencies: `dispatch` repository / aggregate factory、`shop` order context
- Invariants: 相同來源訂單不可建立多筆自動 dispatch 訂單

### Component 2: `EscortDispatchOrder`
- Responsibility: 成為護航工作流的唯一 durable aggregate，保存來源購買脈絡
- Inputs: 手動派單資料或自動 handoff payload
- Outputs: 可查詢、可通知、可售後延續的 dispatch state
- Dependencies: `dispatch` persistence / services
- Invariants: 來源脈絡快照一旦寫入，不受後續 product 編輯或刪除影響

### Component 3: `ShopAdminNotificationService`
- Responsibility: 從 durable dispatch/order state 產生提醒訊息
- Inputs: dispatch order reference 或對應 query result
- Outputs: admin DM / panel notice
- Dependencies: `dispatch` query surface、Discord notification adapter
- Invariants: 通知失敗不得回滾已完成的 durable handoff

## Sequence / Control Flow
1. 貨幣購買成功或法幣 post-payment worker 完成付款真相確認
2. `shop` 將 escort handoff payload 交給 `dispatch` 擁有的 handoff service
3. handoff service 冪等建立 dispatch order，寫入來源購買快照
4. handoff 成功後才產生 admin DM / panel 提醒
5. 後續護航確認、完單、售後都只依賴 dispatch aggregate

## Data / State Impact
- Created or updated data: `escort_dispatch_order` 或相鄰表需要新增來源訂單 / 商品 / option / 金額快照欄位；可能新增 source type enum
- Consistency rules: 自動派單的 canonical source 為 dispatch durable record；通知只是 projection
- Migration / rollout needs: additive migration；舊通知路徑在 handoff 上線前可暫時保留，但完成後需降級為派生邏輯

## Risk and Tradeoffs
- Key risks: 手動派單與自動派單欄位共存設計不佳、duplicate handoff、dispatch UI 尚未顯示新欄位
- Rejected alternatives:
  - 持續沿用 admin DM：無 durable state，無法解決 issue
  - 在 `shop` 保存另一套 escort work item：會形成平行 aggregate
- Operational constraints: 若法幣 worker 仍可重試，handoff 必須以 source order 為冪等鍵

## Validation Plan
- Tests: `UT` handoff service、`IT` repository/migration、重放與 DM failure regression
- Contract checks: `N/A`
- Rollback / fallback: 如需回退，可暫時停用自動 handoff 並保留既有通知，但不得在同一版本長期維持雙重 canonical source

## Open Questions
None
