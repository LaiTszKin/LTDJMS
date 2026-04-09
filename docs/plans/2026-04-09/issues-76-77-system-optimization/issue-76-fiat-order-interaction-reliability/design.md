# Design: Issue 76 法幣下單互動可靠性

- Date: 2026-04-09
- Feature: Issue 76 法幣下單互動可靠性
- Change Name: issue-76-fiat-order-interaction-reliability

## Design Goal
把法幣下單從「同步建單完成後才第一次回覆 interaction」改成「先 deferred acknowledge，再以同一個 interaction hook 回填最終摘要」，同時以 handler 內的短生命週期 guard 阻止同一筆互動窗口重入。

## Change Summary
- Requested change: 依 issue #76 改善法幣下單成功後的使用者通知與互動回覆。
- Existing baseline: `ShopSelectMenuHandler.handleFiatOrderSelect(...)` 先同步呼叫 `FiatOrderService.createFiatOnlyOrder(...)`，再嘗試 DM 成功／失敗後才 `event.reply(...)`。
- Proposed design delta: handler 改成先 `deferReply(true)`，再建單、送 DM，最後用 hook 編輯原始回覆；同時引入 `guildId:userId:productId` 的 in-flight guard。

## Scope Mapping
- Spec requirements covered: `R1.1-R1.2`, `R2.1-R2.2`, `R3.1-R3.3`
- Affected modules:
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/test/java/ltdjms/discord/shop/commands/ShopSelectMenuHandlerTest.java`
  - `src/test/java/ltdjms/discord/shop/services/FiatOrderServiceTest.java`
- External contracts involved: `Discord Interactions`
- Coordination reference: `../coordination.md`

## Current Architecture
現有法幣下單流程如下：
1. select menu 事件進入 `ShopSelectMenuHandler.handleFiatOrderSelect(...)`。
2. handler 直接同步呼叫 `FiatOrderService.createFiatOnlyOrder(...)`。
3. 若建單成功，再嘗試 `openPrivateChannel()` 與 `sendMessage(order.formatDirectMessage())`。
4. DM 成功時只回 `✅ 已將超商代碼與訂單編號私訊給你`；DM 失敗時只回簡短警告。

這個流程的問題是：
- ECPay 取號 + Discord DM 任何一步變慢，都可能超過 Discord interaction 3 秒時限。
- 使用者在 interaction 內拿不到足以查單的摘要。
- 同一使用者連點相同商品時，handler 沒有互動層防重入。

## Proposed Architecture
- 由 `ShopSelectMenuHandler` 擁有 interaction lifecycle：先 defer，再用 hook 回填成功／失敗內容。
- `FiatOrderService` 仍只負責建單與回傳訂單資料，不負責 Discord 文案或 interaction state。
- 新增 handler 內的 in-flight key set，作為單次互動窗口的重入保護；不碰 repository 或資料表 dedupe。

## Component Changes

### Component 1: `ShopSelectMenuHandler`
- Responsibility: 協調法幣下單 interaction、私訊結果與防重入。
- Inputs: `StringSelectInteractionEvent`、`guildId`、`userId`、`productId`
- Outputs: deferred reply、hook edit、Discord DM、對 `FiatOrderService` 的呼叫
- Dependencies: `FiatOrderService`、Discord interactions / JDA callback chain
- Invariants:
  - 長流程前必須先完成 deferred reply。
  - 同一個 in-flight key 不可建立第二張新訂單。
  - 成功與失敗結束時都必須釋放 guard。

### Component 2: `FiatOrderService`
- Responsibility: 維持既有法幣建單與訂單結果資料格式。
- Inputs: `guildId`、`userId`、`productId`
- Outputs: `FiatOrderResult`
- Dependencies: `ProductService`、`EcpayCvsPaymentService`、`FiatOrderRepository`
- Invariants:
  - 不改變 ECPay request、order persistence 與 `FiatOrderResult` 的核心欄位。
  - 仍由服務層決定建單成功或失敗，但不直接處理 interaction acknowledgement。

## Sequence / Control Flow
1. handler 生成 `guildId:userId:productId` guard key；若已存在，立即回「處理中」提示。
2. guard key 成功加入後，對 interaction 執行 `deferReply(true)`。
3. deferred success callback 中呼叫 `FiatOrderService.createFiatOnlyOrder(...)`。
4. 建單成功後嘗試送 DM；不論 DM 成功或失敗，都以 hook 編輯原始 deferred 訊息，填入對應摘要。
5. 任一 terminal path（成功、失敗、例外、DM 失敗）都要釋放 guard key。

## Data / State Impact
- Created or updated data: `None`（不新增 schema、cache 或 config）
- Consistency rules:
  - in-flight guard 只保護單一 handler 生命週期，不替代 repository idempotency。
  - 成功／失敗訊息組裝不得改變 `FiatOrderService` 的持久化副作用。
- Migration / rollout needs: `None`

## Risk and Tradeoffs
- Key risks:
  - defer 成功後若內部例外沒有妥善回填，使用者會只看到長時間 loading。
  - guard 若未釋放，後續下單會被誤擋。
- Rejected alternatives:
  - 重用既有未付款訂單：可減少重複建單，但會改變資料層語意與客服查詢預期，不適合和 interaction 修正綁在同一 spec。
  - 把訂單摘要組裝搬進 `FiatOrderService`：interaction 成功／失敗提示屬 Discord handler 職責，放在 service 會混入 UI 協調邏輯。
- Operational constraints:
  - 需維持現有 DM 文案可讀性，但 interaction 內也要留最小可查單資訊。
  - 不可影響其他貨幣購買或 shop select menu 分支。

## Validation Plan
- Tests:
  - Unit：deferred reply、成功摘要、DM failure fallback、in-flight guard
  - Regression：既有 `FiatOrderService` 建單測試不回歸
  - Adversarial：重複點擊相同商品時不會重複呼叫建單服務
- Contract checks: 以 Discord 官方 interaction 文件確認 initial response 3 秒與 follow-up / edit-original 語意，對照 handler 的 deferred 設計。
- Rollback / fallback: 若回歸風險過高，可回退到舊 handler 邏輯；不會破壞已建立的 `fiat_order` 資料。

## Open Questions
None
