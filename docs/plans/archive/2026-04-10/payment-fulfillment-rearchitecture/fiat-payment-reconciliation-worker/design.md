# Design: Fiat payment reconciliation worker

- Date: 2026-04-10
- Feature: Fiat payment reconciliation worker
- Change Name: fiat-payment-reconciliation-worker

## Design Goal
把付款成功狀態的落庫與發貨/通知副作用拆開，讓任何 paid 訂單都能由資料庫驅動的背景 worker 補做完成。

## Change Summary
- Requested change: 付款成功後不再直接在 callback 裡發貨，而改由背景流程處理，並加入 ECPay 補償查單
- Existing baseline: `FiatPaymentCallbackService` 目前在 paid callback 中直接做管理員通知、後端履約與 fulfilled 標記
- Proposed design delta: callback 只負責 `PAID` transition；背景 scheduler + worker 負責 post-payment actions；另一個 reconciliation scheduler 查詢遺失 callback 的 pending 訂單

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `shop/services`, `shop/persistence`, `db/migration`, `Dagger wiring`
- External contracts involved: `ECPay Payment Callback`, `ECPay QueryTradeInfo / Merchant Trade Query`
- Coordination reference: `../coordination.md`

## Current Architecture
- `EcpayCallbackHttpServer` 接收 callback 後呼叫 `FiatPaymentCallbackService`
- `FiatPaymentCallbackService` 在 `markPaidIfPending()` 後直接呼叫 `handlePostPayment()`
- `handlePostPayment()` 內同步進行管理員通知、外部履約、fulfilled claim / release
- 若 callback 沒送達，系統目前沒有官方查單補償路徑

## Proposed Architecture
- 保留 `markPaidIfPending()` 作為付款真相的單一轉換入口
- 新增 `PostPaidFiatOrderWorker`（名稱待實作）定期抓取 `PAID` 且後處理未完成的訂單
- 新增 `FiatPaymentReconciliationService` 定期查詢 `PENDING_PAYMENT` 且仍在補償視窗內的訂單
- 將 callback thread 中的副作用移出，只保留 buyer DM 這類可選通知或將其也納入 worker

## Component Changes

### Component 1: `FiatPaymentCallbackService`
- Responsibility: 驗證 callback，將訂單轉為 `PAID`，必要時喚醒後處理
- Inputs: callback payload, content-type
- Outputs: `fiat_order` paid transition, callback payload 更新
- Dependencies: `FiatOrderRepository`, ECPay callback contract
- Invariants: paid transition 必須冪等；不可在 callback thread 內做不可控的長耗時副作用

### Component 2: `PostPaidFiatOrderWorker`
- Responsibility: 對 `PAID` 訂單完成管理員通知、護航開單、fulfilled 標記與失敗重試
- Inputs: paid but unfinished orders
- Outputs: admin notified / escort order created / fulfilled timestamps and retry metadata
- Dependencies: `FiatOrderRepository`, shop/admin/dispatch services
- Invariants: 同一筆訂單同一時刻只能由一個 worker claim；副作用成功前不得標 fulfilled

### Component 3: `FiatPaymentReconciliationService`
- Responsibility: 查詢仍為 `PENDING_PAYMENT` 的訂單在 ECPay 官方端是否已付款
- Inputs: pending orders in reconciliation window
- Outputs: paid transition or deferred retry
- Dependencies: ECPay query contract, `FiatOrderRepository`
- Invariants: 只依官方 confirmed paid 狀態轉移；query failure 不改訂單

## Sequence / Control Flow
1. callback 到達並驗證成功後，原子更新 `fiat_order` 為 `PAID`
2. paid callback 回應 `1|OK`，不再同步完成長耗時發貨
3. worker 週期性選出 `PAID` 且未完成的訂單並 claim
4. worker 依序執行管理員通知、護航開單與 fulfilled 更新；失敗時寫回可重試狀態
5. reconciliation scheduler 週期性查詢仍為 `PENDING_PAYMENT` 的訂單；若官方確認 paid，重用同一 paid transition

## Data / State Impact
- Created or updated data: `fiat_order` 需要新增/調整 retry、next_attempt、claim 或等價欄位
- Consistency rules: `PAID` 是付款真相；後處理完成狀態與 retry 狀態不得覆蓋付款真相
- Migration / rollout needs: 新 migration 調整 `fiat_order` 處理欄位；舊 callback 內同步發貨邏輯需下線

## Risk and Tradeoffs
- Key risks: worker claim race、callback/query 同時命中、護航開單失敗導致 fulfilled 卡住
- Rejected alternatives:
  - 只靠 callback：無法補償遺失 callback
  - 只靠 PostgreSQL `LISTEN/NOTIFY`：不適合作為唯一可靠投遞保證
- Operational constraints: scheduler interval 不可過密；查單需考慮 ECPay 配額與延遲

## Validation Plan
- Tests: `UT`, `IT`, mock-based reconciliation matrix, replay/adversarial tests
- Contract checks: 驗證 callback 重送與 query failure 都不會造成錯誤 paid / fulfilled
- Rollback / fallback: 可先保留 schema 新欄位但停用 scheduler；若 worker 有問題仍保留 paid truth

## Open Questions
- 已解答：buyer paid notification 已完全移入 worker，callback thread 不再承擔任何可重試副作用
