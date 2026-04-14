# Design: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot
- Change Name: fiat-order-fulfillment-snapshot

## Design Goal
讓 `fiat_order` 從「付款紀錄」升級成「付款 + 履約契約」的 canonical aggregate，post-payment worker 只需重播訂單自身資料。

## Change Summary
- Requested change: 修正已建立法幣訂單在付款後仍依賴 mutable `product` 主檔履約的問題
- Existing baseline: `FiatOrderService` 只保存 `product_id/name/amount_twd`；`FiatOrderPostPaymentWorker` 每次付款後重新讀取 live `Product`
- Proposed design delta: 在建單當下寫入履約快照；worker 以 snapshot replay 完成 reward / escort handoff / 通知

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `shop`, `product`, 相關 migration / tests / docs
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- `fiat_order` 目前只保存付款所需基本欄位
- 商品是否有 reward、是否自動護航、使用哪個 escort option 都在付款後才從 live product 重新決定
- 因此商品被修改或刪除後，既有 pending order 的履約結果也會跟著漂移

## Proposed Architecture
- `FiatOrder.createPending(...)` 改為接受完整的履約快照值物件或等價欄位集
- `FiatOrderPostPaymentWorker` 只依賴 order snapshot 驗證與執行 reward / escort handoff / 通知
- `ProductService` 僅在建單前提供「建立契約」所需的輸入，不再在 paid replay 階段作為真相來源

## Component Changes

### Component 1: `FiatOrder`
- Responsibility: 保存付款真相與履約契約快照
- Inputs: guildId、buyerUserId、product display snapshot、reward snapshot、escort snapshot、pricing snapshot
- Outputs: 可供 callback / worker /查詢使用的單一 order aggregate
- Dependencies: `shop` persistence
- Invariants: 一旦建單成功，履約快照不可被 product 後續修改覆蓋

### Component 2: `FiatOrderService`
- Responsibility: 在建單當下驗證商品狀態並寫入完整契約快照
- Inputs: live product（只限建單當下）、guild/user context
- Outputs: 完整的 pending `fiat_order`
- Dependencies: `ProductService`, `FiatOrderRepository`
- Invariants: 若履約必要資料不完整，建單直接失敗

### Component 3: `FiatOrderPostPaymentWorker`
- Responsibility: 重播 order snapshot 並冪等完成付款後副作用
- Inputs: paid `fiat_order`
- Outputs: buyer notified / reward granted / escort handed off / fulfilled timestamps
- Dependencies: `FiatOrderRepository`, reward/dispatch handoff services
- Invariants: 不再以 live product row 決定履約

## Sequence / Control Flow
1. `FiatOrderService` 從 live product 讀取一次建單所需資料，組成履約快照
2. `fiat_order` 寫入快照欄位並進入 `PENDING_PAYMENT`
3. callback / reconciliation 將訂單轉為 `PAID`
4. post-payment worker 讀取 order snapshot，完成 reward / escort / 通知副作用
5. 商品後續編輯或刪除只影響新訂單，不影響既有訂單 replay

## Data / State Impact
- Created or updated data: `fiat_order` 新增 reward / escort / display snapshot 欄位；可能需要 value object 或欄位群
- Consistency rules: snapshot 是履約真相；`product_id` 只作追溯輔助，不得再作為 paid replay 的唯一輸入
- Migration / rollout needs: additive migration + 舊資料處理策略；不能讓無 snapshot 的舊 pending order 靜默落入新 worker

## Risk and Tradeoffs
- Key risks: 快照欄位設計過度耦合現行 reward 類型、舊資料 backfill 不完整、worker 與 escort handoff spec 欄位重疊
- Rejected alternatives:
  - 繼續讀 live product：無法解決 issue
  - 將整個 Product JSON 複製進 order：欄位過度寬鬆且缺乏清楚契約邊界
- Operational constraints: migration 必須對舊 pending / paid 資料有明確策略，不能靠隱性 null 容忍

## Validation Plan
- Tests: `UT` 建單快照建立、`IT` 商品修改/刪除後付款 replay、replay idempotency tests
- Contract checks: `N/A`
- Rollback / fallback: 可先 additive 寫入快照並讓新 worker支援雙讀，但最終需收斂到 snapshot-only replay

## Open Questions
None
