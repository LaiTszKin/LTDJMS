# Contract: Fiat Order Buyer Notifications

- Date: 2026-04-09
- Feature: Fiat Order Buyer Notifications
- Change Name: fiat-order-buyer-notifications

## Purpose
本變更直接依賴綠界超商代碼取號結果中的付款期限與付款成功 callback 語意，若誤解上游欄位或回推時機，會導致提醒內容與實際付款狀態不一致。

## Dependency Records

### Dependency 1: ECPay CVS Code Generation Response
- Type: `API`
- Version / Scope: `ECPay 站內付 2.0 超商代碼取號`
- Official Source: `https://developers.ecpay.com.tw/?p=27995`
- Why It Matters: 建單私訊中的付款期限與付款代碼都來自取號成功回應，不能自行推算另一套截止時間。
- Invocation Surface:
  - Entry points: `/1.0.0/Cashier/GenPaymentCode`
  - Call pattern: `sync`
  - Required inputs: `MerchantID`, `OrderInfo.ReturnURL`, `CVSInfo.ExpireDate`
  - Expected outputs: `OrderInfo.MerchantTradeNo`, `CVSInfo.PaymentNo`, `CVSInfo.ExpireDate`, `CVSInfo.PaymentURL`
- Constraints:
  - Supported behavior: 取號成功後會回傳超商代碼與實際 `ExpireDate`；商家需用該回傳資料告知消費者付款資訊。
  - Limits: `CVSInfo.ExpireDate` 由商家設定有效分鐘數，實際截止時間由綠界回傳。
  - Compatibility: 需使用與 stage / prod 相符的 MerchantID、HashKey、HashIV 與 callback URL。
  - Security / access: 呼叫需帶商家憑證，且 request / response `Data` 需經既有加解密流程處理。
- Failure Contract:
  - Error modes: API 可能回傳傳輸錯誤、業務錯誤或不完整資料。
  - Caller obligations: 只有在 `PaymentNo` 與 `ExpireDate` 可用時才能組成完整買家付款提醒；缺資料時需維持既有錯誤處理。
  - Forbidden assumptions: 不得假設本地設定分鐘數一定等於綠界最終回傳的顯示截止時間格式。
- Verification Plan:
  - Spec mapping: `R1.1`, `R1.2`
  - Design mapping: `Proposed Architecture`, `Component 1`
  - Planned coverage: `UT-FiatOrderService-01`
  - Evidence notes: 官方回應範例列出 `PaymentNo`、`ExpireDate`、`PaymentURL`，說明付款資訊應以取號結果為主。

### Dependency 2: ECPay Payment Results Notification
- Type: `API`
- Version / Scope: `ReturnURL payment callback`
- Official Source: `https://developers.ecpay.com.tw/16538/`
- Why It Matters: 付款成功買家通知必須只在付款完成通知成功處理後觸發，且需維持回應 `1|OK` 與 duplicate callback 相容性。
- Invocation Surface:
  - Entry points: Merchant `ReturnURL`
  - Call pattern: `webhook`
  - Required inputs: `MerchantTradeNo`, `RtnCode` / `TradeStatus`, 金流結果 payload
  - Expected outputs: 商家驗證成功後回應 `1|OK`，並自行更新訂單狀態與後續副作用
- Constraints:
  - Supported behavior: 綠界於付款完成後以 server POST 對 `ReturnURL` 發送結果通知。
  - Limits: 付款通知可能重送；商家需自行處理 checksum 驗證與冪等。
  - Compatibility: ATM / CVS / BARCODE 都以 ReturnURL callback 作為付款完成結果來源。
  - Security / access: callback 需經既有 shared-secret 與 payload 驗證路徑保護。
- Failure Contract:
  - Error modes: callback 可重送、缺欄位、驗證失敗或 order not found。
  - Caller obligations: 商家收到合法成功 callback 後應回應 `1|OK`，並避免把 duplicate callback 當成新的成功事件。
  - Forbidden assumptions: 不得假設任何 paid callback 只會到達一次，也不得讓額外買家私訊副作用影響既有回應與履約流程。
- Verification Plan:
  - Spec mapping: `R2.1`, `R2.2`, `R2.3`
  - Design mapping: `Sequence / Control Flow`, `Component 2`
  - Planned coverage: `UT-FiatPaymentCallbackService-01..03`
  - Evidence notes: 官方文件明確說明付款完成後會 POST 到 `ReturnURL`，商家應驗證後回覆 `1|OK`。
