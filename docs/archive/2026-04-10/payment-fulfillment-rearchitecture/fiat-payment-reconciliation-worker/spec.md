# Spec: Fiat payment reconciliation worker

- Date: 2026-04-10
- Feature: Fiat payment reconciliation worker
- Owner: Codex

## Goal
讓法幣訂單的付款確認與後續發貨/通知解耦，並在 callback 遺失或副作用失敗時仍可由資料庫驅動的背景流程補償完成。

## Scope

### In Scope
- 保留 ECPay callback 作為付款成功的主訊號，收到成功時立即將 `fiat_order` 轉為 `PAID`
- 新增背景 worker，從已付款但未完成後處理的訂單中冪等執行管理員通知、護航開單與 fulfilled 標記
- 新增補償查單機制，定期查詢仍為 `PENDING_PAYMENT` 的訂單是否已在 ECPay 端付款成功
- 調整 callback 流程，使其不再在 request thread 內直接執行發貨/外部履約
- 新增必要 schema / retry / claim 欄位與測試

### Out of Scope
- 變更 Discord slash command 名稱或訂單編號格式
- 重做整個 shop / dispatch UI
- 引入外部 message queue 或 PostgreSQL `LISTEN/NOTIFY` 作為唯一可靠事件總線

## Functional Behaviors (BDD)

### Requirement 1: Callback 必須只負責付款真相落庫
**GIVEN** ECPay callback 已驗證為對某筆 `fiat_order` 的有效 paid 通知  
**AND** 該訂單目前仍為 `PENDING_PAYMENT`  
**WHEN** callback 被處理  
**THEN** 系統必須立即把訂單狀態更新為 `PAID` 並記錄 callback payload  
**AND** callback 完成後不得在 request thread 內直接執行可重試的發貨副作用

**Requirements**:
- [ ] R1.1 paid callback 成功時，`fiat_order.status` 必須原子地從 `PENDING_PAYMENT` 轉為 `PAID`
- [ ] R1.2 重複 paid callback 不得重複建立發貨工作或重複執行副作用
- [ ] R1.3 buyer paid notification 若保留，失敗只可記錄並重試/補償，不得回滾付款狀態

### Requirement 2: 背景 worker 必須可冪等完成付款後處理
**GIVEN** 某筆訂單已經是 `PAID`  
**AND** 管理員通知、護航開單或 fulfilled 狀態尚未完成  
**WHEN** 背景 worker 取得該筆訂單的處理 claim  
**THEN** 系統必須只由單一 worker 執行一次中的一次嘗試  
**AND** 成功後更新對應完成欄位，失敗時保留可重試狀態

**Requirements**:
- [ ] R2.1 背景 worker 必須使用資料庫 claim 機制避免重複處理同一筆訂單
- [ ] R2.2 管理員通知與護航開單需各自具備完成標記與失敗後可重試能力
- [ ] R2.3 fulfilled 只能在必要副作用成功完成後標記，不得先標記後處理

### Requirement 3: 補償查單必須能回收遺失 callback
**GIVEN** 某筆訂單仍為 `PENDING_PAYMENT`  
**AND** callback 可能因網路或上游狀態未送達  
**WHEN** 補償排程查詢 ECPay 官方查單 API  
**THEN** 若官方狀態顯示已付款，系統必須將其轉成 `PAID` 並交由背景 worker 後續處理  
**AND** 若仍未付款或查單失敗，不得錯誤改變訂單狀態

**Requirements**:
- [ ] R3.1 補償查單只可對符合條件的 `PENDING_PAYMENT` 訂單執行
- [ ] R3.2 官方查單確認 paid 時，狀態轉換與 callback paid transition 必須共用同一套資料層冪等規則
- [ ] R3.3 查單失敗、超時或回傳未付款時，不得錯誤觸發後續發貨

## Error and Edge Cases
- [x] callback 重送、worker 重跑、排程重疊時不可重複發貨
- [x] 商品或護航選項被刪除時，worker 必須保留失敗原因且不可把 fulfilled 標成成功
- [x] ECPay 查單暫時失敗、逾時或回傳資料不一致時，只能延後重試
- [x] buyer DM / 管理員通知失敗不得污染付款真相
- [x] callback 與補償查單同時命中同一筆訂單時，最後只能落成單一 paid transition

## Completion Status

- [x] R1.1 paid callback 成功時，`fiat_order.status` 會透過 `markPaidIfPending()` 原子地從 `PENDING_PAYMENT` 轉為 `PAID`
- [x] R1.2 重複 paid callback 只更新 callback 狀態，不會重複建立或執行副作用
- [x] R1.3 buyer paid notification 已移入背景 worker，失敗只釋放處理 claim，不回滾付款狀態
- [x] R2.1 背景 worker 以資料庫 claim 機制避免重複處理同一筆訂單
- [x] R2.2 buyer/admin/reward 皆有獨立完成標記，可在後續重試補完
- [x] R2.3 fulfilled 僅在必要副作用完成後才會標記
- [x] R3.1 reconciliation 只掃描條件符合的 `PENDING_PAYMENT` 訂單
- [x] R3.2 reconciliation paid transition 重用 `markPaidIfPending()` 的同一套冪等規則
- [x] R3.3 查單失敗或未付款只會排入下次重試，不會誤觸發後續處理

## Clarification Questions
None

## References
- Official docs:
  - https://developers.ecpay.com.tw/16449/
  - https://developers.ecpay.com.tw/16538/
  - https://developers.ecpay.com.tw/?p=2878
- Related code files:
  - `src/main/java/ltdjms/discord/shop/services/FiatPaymentCallbackService.java`
  - `src/main/java/ltdjms/discord/shop/persistence/JdbcFiatOrderRepository.java`
  - `src/main/java/ltdjms/discord/shop/domain/FiatOrder.java`
