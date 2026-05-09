# Design: Fiat Order Buyer Notifications

- Date: 2026-04-09
- Feature: Fiat Order Buyer Notifications
- Change Name: fiat-order-buyer-notifications

## Design Goal
在不改變既有法幣建單、付款成功狀態轉移與履約／管理員通知語意的前提下，補上一條買家私訊通知路徑，讓建單提醒與付款成功提醒都以既有訂單資料為唯一真相來源。

## Change Summary
- Requested change: 建單後提醒買家付款期限與自動取消風險，並在付款成功後再私訊買家附上訂單編號。
- Existing baseline: `ShopSelectMenuHandler` 目前只在建單成功後傳送付款資訊 DM；`FiatPaymentCallbackService` 只做 paid transition、管理員通知與 fulfillment，未通知買家。
- Proposed design delta: 保留建單路徑既有 DM 傳送方式，只補強付款期限提醒文案；另外新增一個專門處理 paid callback 買家私訊的 service，避免 callback service 直接操作 JDA 細節。

## Scope Mapping
- Spec requirements covered: `R1.1`-`R2.3`
- Affected modules:
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatPaymentCallbackService.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderBuyerNotificationService.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shared/di/CommandHandlerModule.java`
  - `src/test/java/ltdjms/discord/shop/services/FiatOrderServiceTest.java`
  - `src/test/java/ltdjms/discord/shop/services/FiatPaymentCallbackServiceTest.java`
  - `src/test/java/ltdjms/discord/shop/commands/ShopSelectMenuHandlerTest.java`
- External contracts involved:
  - `ECPay CVS Code Generation Response`
  - `ECPay Payment Results Notification`
- Coordination reference: `None`

## Current Architecture
- `FiatOrderService.createFiatOnlyOrder(...)` 會查商品、呼叫 `EcpayCvsPaymentService.generateCvsPaymentCode(...)`、建立 `FiatOrder`，並回傳 `FiatOrderResult` 給 interaction handler。
- `FiatOrderResult.formatDirectMessage()` 原本輸出商品、訂單編號、超商代碼、金額、付款期限與簡短付款提示，但未提醒逾期取消風險。
- `ShopSelectMenuHandler` 在建單成功後直接用 `user.openPrivateChannel().sendMessage(...)` 傳給買家，DM 失敗時回退到 deferred interaction 內文。
- `FiatPaymentCallbackService.handlePostPayment(...)` 僅負責 paid 後的 admin notify 與 fulfillment claim path，沒有買家 DM。

## Proposed Architecture
- 建單成功時仍由 `ShopSelectMenuHandler` 控制 interaction fallback 與 JDA DM 傳送，但 `FiatOrderResult` 與 interaction 摘要文案都補上付款期限內完成付款、逾期自動取消提醒。
- 新增 `FiatOrderBuyerNotificationService`，集中處理 paid callback 後的買家成功通知、Discord user lookup 與失敗 logging。
- 付款成功 callback 只在 `markPaidIfPending(...)` 成功取得首次 paid transition 後呼叫買家通知 service；duplicate paid callback 不進入該路徑。
- 買家通知 service 的失敗採 swallow + warn log，絕不回滾 repository state，也不改變既有 admin / fulfillment 執行順序。

## Component Changes

### Component 1: `FiatOrderService.FiatOrderResult`
- Responsibility: 提供建單完成後的 canonical 買家付款資訊文案。
- Inputs: `product`, `orderNumber`, `paymentNo`, `expireDate`, `paymentUrl`
- Outputs: 建單成功 DM 文案字串
- Dependencies: `ECPay CVS Code Generation Response`
- Invariants:
  - 文案必須顯示訂單編號。
  - 若 `expireDate` 存在，提醒文字不得與該到期資訊矛盾。

### Component 2: `FiatOrderBuyerNotificationService`
- Responsibility: 封裝 paid callback 買家 DM 的 JDA 呼叫、付款成功通知文案與失敗 logging。
- Inputs: `FiatOrder`
- Outputs: Discord DM side effect、warn log
- Dependencies: `JDAProvider`, `ECPay Payment Results Notification`
- Invariants:
  - 付款成功通知必須附上訂單編號。
  - DM 失敗不得丟出未處理例外給主流程。

## Sequence / Control Flow
1. `ShopSelectMenuHandler` 建單成功後取得 `FiatOrderResult`，沿用既有 JDA DM 傳送付款資訊，但 DM 與 interaction 摘要都包含付款期限內完成付款與逾期取消提醒。
2. 若建單 DM 失敗，handler 維持既有 deferred interaction fallback，讓使用者仍能看到付款資訊。
3. `FiatPaymentCallbackService` 在首次成功 `markPaidIfPending(...)` 並完成既有 post-payment side effects 後，呼叫買家通知 service 傳送付款成功通知。
4. duplicate paid callback 仍只更新 callback status，不重複傳送付款成功通知。

## Data / State Impact
- Created or updated data: 無新增 schema；僅更新買家 DM 文案與 callback 副作用編排。
- Consistency rules:
  - `fiat_order` paid 狀態仍是付款成功通知是否送出的唯一 gating truth。
  - 付款成功買家通知只在首次 `markPaidIfPending(...)` 成功後觸發，以維持 deduplication。
  - 建單提醒中的付款期限顯示以 ECPay 回傳 `ExpireDate` 為主，不新增本地計算欄位。
- Migration / rollout needs: `None`

## Risk and Tradeoffs
- Key risks:
  - Discord DM 失敗造成 callback path 拋錯。
  - duplicate callback 重複通知買家。
  - 建單提醒與 interaction fallback 文案不同步，導致 DM 關閉時遺失取消提醒。
- Rejected alternatives:
  - 直接在 `FiatPaymentCallbackService` 內寫 JDA DM 細節：會讓 callback service 同時承擔業務判斷與 Discord I/O，且難以單獨 mock。
  - 以本地 `payment.ecpay.cvs-expire-minutes` 自行格式化截止時間：可能與綠界實際回傳 `ExpireDate` 不一致。
- Operational constraints:
  - 需維持 callback 處理最終仍回 `1|OK`。
  - 不新增背景排程或新 config key，部署面維持不變。

## Validation Plan
- Tests:
  - `UT-FiatOrderService-01`：驗證建單私訊文案。
  - `ShopSelectMenuHandlerTest`：驗證 interaction 成功與 DM failure fallback 仍包含取消提醒。
  - `UT-FiatPaymentCallbackService-01`：首次付款成功 callback 會通知買家且保留既有副作用。
  - `UT-FiatPaymentCallbackService-02`：買家 DM 失敗不影響 paid path。
  - `UT-FiatPaymentCallbackService-03`：duplicate paid callback 不重複通知。
  - Property-based：`N/A`，因本次無新的生成型商業邏輯不變式。
- Contract checks:
  - 建單提醒以 mock `CvsPaymentCode.expireDate` 驗證文案使用上游資料。
  - callback 測試維持 `markPaidIfPending` / duplicate paid path 分支，對齊 ReturnURL callback 語意。
- Rollback / fallback:
  - 若新買家通知 service 出現問題，可移除 service 注入並退回原本只有建單 DM 的行為，不影響資料庫 schema。

## Open Questions
None
