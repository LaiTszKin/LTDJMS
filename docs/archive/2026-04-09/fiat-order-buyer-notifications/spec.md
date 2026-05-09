# Spec: Fiat Order Buyer Notifications

- Date: 2026-04-09
- Feature: Fiat Order Buyer Notifications
- Owner: Codex

## Goal
讓法幣下單買家在建單後與付款成功後都能收到包含訂單編號的私訊通知，並在建單當下明確得知付款期限與逾期取消風險。

## Scope

### In Scope
- 法幣訂單建立成功後，對買家送出私訊提醒，包含訂單編號、付款代碼、金額、付款期限與逾期自動取消提醒。
- 建單通知文案改為明確指出付款期限來自系統設定的超商代碼有效期限。
- 綠界付款成功 callback 到達並完成已付款狀態轉移後，對買家再送出一則私訊通知，附上訂單編號。
- 付款成功私訊失敗時只記錄日誌，不得阻斷既有 paid transition、管理員通知與履約流程。
- 補足單元／回歸測試，涵蓋建單通知與付款成功通知的成功／失敗分支。

### Out of Scope
- 變更 `fiat_order` schema、ECPay payload 格式、MerchantTradeNo 產生規則。
- 新增排程取消訂單或額外逾期狀態；本次只補買家提醒文案。
- 調整管理員通知、履約 webhook 或 `/shop` 互動流程的外部可見內容（除必要文案同步外）。

## Functional Behaviors (BDD)

### Requirement 1: 建單後需私訊付款期限與取消提醒
**GIVEN** 使用者在 guild 內為限定法幣商品建立超商代碼訂單
**AND** 系統已透過 `payment.ecpay.cvs-expire-minutes` 設定有效付款期限，且綠界取號結果已回傳訂單編號、超商代碼與到期時間
**WHEN** 訂單成功建立並準備送出買家私訊
**THEN** 私訊內容必須包含商品名稱、訂單編號、超商代碼、金額與付款期限
**AND** 私訊必須提醒使用者需在期限內完成付款，否則訂單將被自動取消

**Requirements**:
- [x] R1.1 建單成功私訊需顯示訂單編號，且不得省略付款期限欄位（只要取號結果提供到期時間）。
- [x] R1.2 建單成功私訊需包含明確提醒：「請在付款期限內完成付款，否則訂單將被自動取消」或等價語意。
- [x] R1.3 互動層既有成功／失敗 fallback 行為需保持相容，不得因新增提醒而改變 DM 失敗時的可付款備援資訊。

### Requirement 2: 付款成功後需對買家送出完成通知
**GIVEN** 綠界付款成功 callback 已通過既有驗證與冪等狀態轉移
**AND** 訂單存在且其 `buyerUserId` 可對應 Discord 使用者
**WHEN** 系統處理 paid callback 的後續副作用
**THEN** 系統必須額外嘗試私訊買家一則付款成功通知，並附上訂單編號
**AND** 該通知不得破壞既有管理員通知、履約 claim 與 duplicate callback 保護

**Requirements**:
- [x] R2.1 第一次成功的 paid transition 應送出一次買家付款成功私訊，內容至少包含訂單編號與付款完成語意。
- [x] R2.2 若收到重複 paid callback，不得因 callback 重送而重複對買家送出付款成功私訊。
- [x] R2.3 若買家私訊開啟或送出失敗，只能記錄警告日誌，不得回滾訂單狀態、管理員通知或履約流程。

## Error and Edge Cases
- [x] 建單 DM 開啟失敗或送出失敗時，interaction 仍須保留完整付款資訊。
- [x] 付款成功 callback 若為 duplicate / replay，不得重複對買家私訊成功通知。
- [x] 付款成功 callback 若找不到商品或 Discord 使用者，既有 callback 回應仍須成功完成，不得卡住 paid path。
- [x] 付款期限提醒需以既有系統設定與綠界回傳到期時間為主，不得自行推算另一份不一致的截止時間。
- [x] Discord DM 相關失敗不得中斷管理員通知與 fulfillment claim / release 邏輯。

## Clarification Questions
None

## References
- Official docs:
  - https://developers.ecpay.com.tw/?p=27995
  - https://developers.ecpay.com.tw/16538/
- Related code files:
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatPaymentCallbackService.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderBuyerNotificationService.java`
