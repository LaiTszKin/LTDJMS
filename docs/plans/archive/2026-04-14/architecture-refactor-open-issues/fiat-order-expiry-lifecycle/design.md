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
- 建單文案已告知買家逾期會自動轉為逾期取消狀態
- `fiat_order` 已具備 `PENDING_PAYMENT` / `PAID` / `EXPIRED` 狀態，以及 `expire_at` / `expired_at` / `terminal_reason`
- reconciliation 先做 expiry sweep，再只對仍在有效期內的 pending order 掃描查單，避免超時後殘留 `PENDING_PAYMENT`

## Proposed Architecture
- `FiatOrder` 以 `expireAt` 作為 canonical 到期依據，並保存 `expiredAt` 與 `terminalReason`
- `EcpayCvsPaymentService` 直接把 `ExpireDate` 轉成 `Instant expireAt`；若上游資料不可解析，退回到 request-time + configured expiry minutes
- `FiatPaymentReconciliationService` 先掃描過期 pending order 並轉成 `EXPIRED`，再只對仍在有效期內的 pending order 進行 QueryTradeInfo 補償查單
- paid callback / query / expiry transition 採 conditional update，讓 first-writer-wins 決定單一 final truth

## Component Changes

### Component 1: `FiatOrder`
- Responsibility: 表示 pending / paid / expired 的完整生命周期
- Inputs: 建單期限資訊、paid callback/query 結果、expiry transition
- Outputs: 一致的 order status 與時間欄位
- Dependencies: `shop` persistence
- Invariants: `PAID` 與 `EXPIRED` 不可同時成立；pending order 必須有明確有效期；expired order 必須有 `expiredAt` / `terminalReason`

### Component 2: `JdbcFiatOrderRepository`
- Responsibility: 提供 pending selection、expiry transition、paid transition 的原子狀態更新
- Inputs: current time、order number、query/callback result
- Outputs: selection rows、狀態更新結果
- Dependencies: PostgreSQL schema
- Invariants: terminal state 一旦寫入，不再被 reconciliation 選中；losing paid race must release the claim

### Component 3: `FiatPaymentReconciliationService` / expiry scheduler
- Responsibility: 在有效期內查單，逾期時做 terminal transition
- Inputs: pending orders with `expireAt`
- Outputs: `PAID` transition、`EXPIRED` transition 或下次重試
- Dependencies: ECPay query contract、repository
- Invariants: callback/query/expiry race 只能落成單一最終真相；expiry sweep 先於 reconciliation

## Sequence / Control Flow
1. 建單時保存上游付款期限到 `fiat_order`
2. scheduler 先 sweep 已逾期 pending orders，將其標記為 `EXPIRED`
3. reconciliation 只挑仍在有效期內的 pending orders 做 QueryTradeInfo 補償查單
4. 若 callback/query 在有效期內確認付款，走 paid transition；若與 expiry race，同步靠原子條件更新決定唯一結果
5. 查詢與文件統一把 terminal state 呈現為逾期/取消，而非模糊 pending

## Data / State Impact
- Created or updated data: `fiat_order.status` enum / check constraint、`expire_at`、`expired_at`、`terminal_reason`
- Consistency rules: 到期時間是 lifecycle 判斷依據；7 天 reconciliation window 只是操作策略，不能代替 lifecycle state
- Migration / rollout needs: additive migration + 舊 pending order fallback；`V025__add_fiat_order_expiry_lifecycle.sql` backfills `expire_at` from `created_at + 7 days`

## Risk and Tradeoffs
- Key risks: historical rows 沒有 `expireAt`、paid/expired race、營運查詢仍假設只有兩種狀態
- Rejected alternatives:
  - 只更新文件：無法修正資料模型
  - 繼續用 reconciliation window 代表到期：語意錯誤且不可觀測
- Operational constraints: expiry scheduler 與 reconciliation 不可彼此重複掃描同一筆 terminal order；losing paid race must release claim

## Validation Plan
- Tests: `UT` 狀態轉移、`IT` near-deadline race、歷史資料 fallback、文件回歸檢查
- Contract checks: 驗證本地保存的期限與 ECPay 文件一致，且 QueryTradeInfo 只在有效 pending 上使用
- Evidence: `FiatOrderTest`, `FiatPaymentReconciliationServiceTest`, `FiatPaymentCallbackServiceTest`, `FiatOrderServiceTest`, `JdbcFiatOrderRepositoryIntegrationTest`, `EcpayCvsPaymentServiceTest`
- Rollback / fallback: 若需回退，可保留新欄位但停用 expiry transition；不得回到只靠掃描視窗暗示取消的狀態

## Open Questions
None
