# Contract: Fiat payment reconciliation worker

- Date: 2026-04-10
- Feature: Fiat payment reconciliation worker
- Change Name: fiat-payment-reconciliation-worker

## Purpose
本變更直接依賴 ECPay 的付款 callback 與查單契約；若對 paid 判斷、補償查單條件或冪等假設理解錯誤，就可能造成誤發貨或漏發貨。

## Dependency Records

### Dependency 1: ECPay Payment Callback
- Type: `hosted service`
- Version / Scope: `Not fixed`
- Official Source: `https://developers.ecpay.com.tw/16449/`, `https://developers.ecpay.com.tw/16538/`
- Why It Matters: callback 是法幣訂單 paid transition 的主訊號來源
- Invocation Surface:
  - Entry points: `OrderInfo.ReturnURL`, payment result notification POST
  - Call pattern: `webhook`
  - Required inputs: `MerchantTradeNo`, merchant credentials, reachable callback URL
  - Expected outputs: merchant 接收付款結果並回 `1|OK`
- Constraints:
  - Supported behavior: ECPay 付款完成後會對 merchant callback URL 發送 server-side 通知
  - Limits: callback 可能重送；merchant 必須接受重複通知
  - Compatibility: callback URL 必須可由 ECPay reach 到
  - Security / access: 需使用正確 merchant 環境金鑰驗證 payload
- Failure Contract:
  - Error modes: callback 遺失、重送、payload 驗證失敗、狀態不一致
  - Caller obligations: 冪等處理、不可把單次 callback 當成唯一送達保證
  - Forbidden assumptions: 不可假設 callback 只會送一次，也不可假設 callback 永不遺失
- Verification Plan:
  - Spec mapping: `R1.x`
  - Design mapping: `Callback Persistence Path`
  - Planned coverage: `UT-callback-async-paid`, `IT-paid-transition-once`
  - Evidence notes: callback 是 server-side payment notification，不是 buyer client-side redirect

### Dependency 2: ECPay QueryTradeInfo / Merchant Trade Query
- Type: `hosted service`
- Version / Scope: `Not fixed`
- Official Source: `https://developers.ecpay.com.tw/?p=2878`
- Why It Matters: 補償查單需要用官方查單結果回收遺失 callback 的 paid 訂單
- Invocation Surface:
  - Entry points: merchant trade query API
  - Call pattern: `sync polling`
  - Required inputs: merchant credentials, merchant trade number / order identifier
  - Expected outputs: trade status / paid state / transaction details
- Constraints:
  - Supported behavior: merchant 主動查詢交易狀態
  - Limits: 只可依官方回傳狀態判定是否 paid，不可自行猜測
  - Compatibility: 必須與建立訂單時使用的 merchant 環境一致
  - Security / access: 使用對應環境的 merchant 憑證
- Failure Contract:
  - Error modes: 查單逾時、查無資料、狀態仍未付款、環境不一致
  - Caller obligations: 失敗時保留原狀並延後重試；不得把失敗視為 paid
  - Forbidden assumptions: 不可將 query failure 解讀成付款失敗或成功
- Verification Plan:
  - Spec mapping: `R3.x`
  - Design mapping: `Reconciliation Scheduler`
  - Planned coverage: `UT-ecpay-query-status-matrix`, `IT-reconcile-pending-orders`
  - Evidence notes: 查單 API 是補償訊號來源，不替代 callback 主鏈
