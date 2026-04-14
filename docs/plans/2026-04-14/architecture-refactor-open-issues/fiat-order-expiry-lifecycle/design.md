# Design: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle
- Change Name: fiat-order-expiry-lifecycle

## Design Goal
補齊 `fiat_order` 的未付款終止狀態，讓「待付款」與「逾期已取消」不再共用同一個 `PENDING_PAYMENT` 模糊狀態。

## Change Summary
- Requested change: 讓逾期未付款訂單進入明確 terminal state，並與文案承諾對齊
- Existing baseline: `FiatOrder.Status` 只有 `PENDING_PAYMENT` / `PAID`；scheduler 只在 7 天視窗內查單，超時後訂單只是停止被掃描
- Proposed design delta: 新增 expire-at + terminal status，建立顯式 expiry transition，並讓 reconciliation / docs /查詢全部改用新生命週期

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `shop/domain`, `shop/persistence`, `shop/services`, migration/docs/tests
- External contracts involved: `ECPay B2C 站內付 / CVS 付款期限契約`, `ECPay QueryTradeInfo / 補償查單契約`
- Coordination reference: `../coordination.md`

## Current Architecture
- 建單文案告知買家逾期會自動取消
- domain / schema 沒有任何 expired/cancelled status
- reconciliation 僅以「建立時間距今不超過 7 天」作為查單視窗，導致超時後仍殘留 `PENDING_PAYMENT` 訂單

## Proposed Architecture
- 在 `FiatOrder` 新增付款期限欄位與 terminal status（建議 `EXPIRED`，如有業務需要可再區分 `CANCELLED`）
- 建單時保存上游付款期限；scheduler / query selection 只挑仍未過期的 pending order
- 增加一條顯式 expiry transition：在偵測到當前時間已超過 `expireAt` 且未付款時，把訂單轉成 terminal state

## Component Changes

### Component 1: `FiatOrder`
- Responsibility: 表示 pending / paid / expired 的完整生命周期
- Inputs: 建單期限資訊、paid callback/query 結果、expiry transition
- Outputs: 一致的 order status 與時間欄位
- Dependencies: `shop` persistence
- Invariants: `PAID` 與 `EXPIRED` 不可同時成立；pending order 必須有明確有效期

### Component 2: `JdbcFiatOrderRepository`
- Responsibility: 提供 pending selection、expiry transition、paid transition 的原子狀態更新
- Inputs: current time、order number、query/callback result
- Outputs: selection rows、狀態更新結果
- Dependencies: PostgreSQL schema
- Invariants: terminal state 一旦寫入，不再被 reconciliation 選中

### Component 3: `FiatPaymentReconciliationService` / expiry scheduler
- Responsibility: 在有效期內查單，逾期時做 terminal transition
- Inputs: pending orders with `expireAt`
- Outputs: `PAID` transition、`EXPIRED` transition 或下次重試
- Dependencies: ECPay query contract、repository
- Invariants: callback/query/expiry race 只能落成單一最終真相

## Sequence / Control Flow
1. 建單時保存上游付款期限到 `fiat_order`
2. scheduler 週期性挑選仍在有效期內的 pending orders 做 reconciliation
3. 若訂單已超過 `expireAt` 且仍未付款，走 expiry transition，標記為 terminal state
4. 若 callback/query 在有效期內確認付款，走 paid transition；若與 expiry race，同步靠原子條件更新決定唯一結果
5. 查詢與文件統一把 terminal state 呈現為逾期/取消，而非模糊 pending

## Data / State Impact
- Created or updated data: `fiat_order.status` enum / check constraint、`expire_at`、可選的 `expired_at` 或 `terminal_reason`
- Consistency rules: 到期時間是 lifecycle 判斷依據；7 天 reconciliation window 只是操作策略，不能代替 lifecycle state
- Migration / rollout needs: additive migration + 舊 pending order fallback；必要時對歷史資料補上保守的 `expireAt`

## Risk and Tradeoffs
- Key risks: historical rows 沒有 `expireAt`、paid/expired race、營運查詢仍假設只有兩種狀態
- Rejected alternatives:
  - 只更新文件：無法修正資料模型
  - 繼續用 reconciliation window 代表到期：語意錯誤且不可觀測
- Operational constraints: expiry scheduler 與 reconciliation 不可彼此重複掃描同一筆 terminal order

## Validation Plan
- Tests: `UT` 狀態轉移、`IT` near-deadline race、歷史資料 fallback、文件回歸檢查
- Contract checks: 驗證本地保存的期限與 ECPay 文件一致，且 QueryTradeInfo 只在有效 pending 上使用
- Rollback / fallback: 若需回退，可保留新欄位但停用 expiry transition；不得回到只靠掃描視窗暗示取消的狀態

## Open Questions
None
