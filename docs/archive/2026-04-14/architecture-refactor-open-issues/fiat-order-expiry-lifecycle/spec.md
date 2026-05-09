# Spec: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle
- Owner: Codex

## Goal
讓未付款法幣訂單在超過 ECPay 付款期限後進入明確的 terminal state，讓系統、文件與使用者承諾對「逾期自動取消」有一致的可觀測語義。

## Scope

### In Scope
- 為 `fiat_order` 增加明確的逾期/取消終止狀態與必要時間欄位
- 讓 reconciliation / scheduler 只處理仍可付款的 pending order
- 對齊建單訊息、文件、查詢與測試對逾期狀態的描述

### Out of Scope
- 重做整個 ECPay 下單流程或付款號碼格式
- 變更 paid callback 與 post-payment worker 的核心責任
- 重做客服後台或人工取消功能全集

## Functional Behaviors (BDD)

### Requirement 1: 未付款訂單必須有明確 terminal state
**GIVEN** 系統建立了一筆法幣訂單  
**AND** ECPay 已回傳付款期限 / `ExpireDate`  
**WHEN** 訂單超過付款期限仍未付款  
**THEN** `fiat_order` 必須進入明確的終止狀態（例如 `EXPIRED` 或等價狀態）  
**AND** 系統必須保留可查詢的到期時間與終止原因

**Requirements**:
- [x] R1.1 `FiatOrder.Status` 與 DB constraint 必須納入逾期終止狀態
  - Evidence: `FiatOrder.Status` now includes `EXPIRED`; `V025__add_fiat_order_expiry_lifecycle.sql` widens the check constraint to `PENDING_PAYMENT` / `PAID` / `EXPIRED`.
- [x] R1.2 `fiat_order` 必須保存足以判定何時過期的時間欄位，而不是只靠 7 天補償視窗推斷
  - Evidence: `expire_at`, `expired_at`, and `terminal_reason` are persisted; `EcpayCvsPaymentService` resolves `ExpireDate` into `expireAt` and falls back to request-time arithmetic only when parsing fails.
- [x] R1.3 逾期狀態一旦成立，不得再被當成一般 `PENDING_PAYMENT` 訂單參與營運統計或掃描
  - Evidence: reconciliation selection excludes expired rows, expiry sweep converts overdue rows to `EXPIRED`, and paid-vs-expired race handling releases claims on the losing side.

### Requirement 2: Reconciliation 與排程必須尊重到期邊界
**GIVEN** scheduler 會週期性掃描待付款訂單  
**WHEN** 掃描或查單發生  
**THEN** 只有仍在付款有效期內的 pending order 可以參與 reconciliation  
**AND** 已逾期訂單必須被轉成 terminal state，而不是單純停止掃描後留在 `PENDING_PAYMENT`

**Requirements**:
- [x] R2.1 reconciliation selection query 必須排除已逾期 / 已取消訂單
  - Evidence: `findOrdersPendingReconciliation(...)` filters by `COALESCE(expire_at, created_at + INTERVAL '7 days') > now`.
- [x] R2.2 系統必須有明確的 expiry transition 路徑（scheduler、query side effect 或等價機制）
  - Evidence: `FiatPaymentReconciliationService.expirePendingOrders(...)` sweeps overdue rows before reconciliation and writes terminal state through `markExpiredIfPending(...)`.
- [x] R2.3 callback / query 若在接近截止時與 expiry transition 競爭，必須有一致且可文件化的狀態優先規則
  - Evidence: first-writer-wins on conditional updates; reconciliation releases claims when paid transition loses to a race, and callback logs after-expiry arrivals separately.

### Requirement 3: 使用者與維運文件必須反映真實生命週期
**GIVEN** 建單私訊與文件都承諾「逾期自動取消」  
**WHEN** 本次重構完成  
**THEN** buyer-facing 文案、文件與查詢結果都必須能觀察到 expired/cancelled 狀態  
**AND** 不可再把「超過 7 天不再掃描」描述成自動取消

**Requirements**:
- [x] R3.1 建單私訊與功能文件必須對齊新的 terminal state 用語
  - Evidence: buyer-facing copy in `FiatOrderService`, `ShopSelectMenuHandler`, and docs now describes expiry as an explicit terminal state.
- [x] R3.2 repository / query / panel 顯示邏輯必須能區分 pending、paid、expired/cancelled
  - Evidence: repository maps `EXPIRED`, shop copy shows overdue/cancelled semantics, and current query paths distinguish live pending from terminal orders.
- [x] R3.3 測試必須覆蓋未付款逾期、接近截止付款成功與重複排程三種情境
  - Evidence: unit and integration tests cover overdue expiry, paid transition, paid-vs-expired race, selection exclusion, and repository persistence.

## Error and Edge Cases
- [x] callback / reconciliation 在截止時間前後競爭時，不得產生 `PAID` 與 `EXPIRED` 雙重真相
  - Evidence: conditional status updates ensure only one terminal writer wins; losing paid races release the claim.
- [x] 若上游未回傳可直接解析的截止時間，系統必須明確定義本地 expiry 計算來源
  - Evidence: `EcpayCvsPaymentService` falls back to request time + configured expiry minutes when parsing fails.
- [x] 已逾期訂單不得再被 worker 當成可付款訂單重試查單
  - Evidence: reconciliation selection excludes overdue rows and the unpaid branch moves overdue orders directly to `EXPIRED`.
- [x] 歷史資料若缺少到期時間欄位，需要有明確 backfill / fallback 策略
  - Evidence: `V025__add_fiat_order_expiry_lifecycle.sql` backfills `expire_at` from `created_at + 7 days`.
- [x] 到期 transition 失敗時不得讓同一筆訂單永遠卡在模糊狀態
  - Evidence: expiry update is idempotent and clears `reconciliation_processing_at`, while reconciliation releases claims on lost paid races.

## Clarification Questions
None

## References
- Official docs:
  - https://developers.ecpay.com.tw/16449/
  - https://developers.ecpay.com.tw/16538/
- Related code files:
  - `src/main/java/ltdjms/discord/shop/domain/FiatOrder.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatPaymentReconciliationService.java`
  - `src/main/java/ltdjms/discord/shop/persistence/JdbcFiatOrderRepository.java`
  - `src/main/resources/db/migration/V025__add_fiat_order_expiry_lifecycle.sql`
