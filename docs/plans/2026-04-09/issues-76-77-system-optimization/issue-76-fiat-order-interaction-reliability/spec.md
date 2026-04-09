# Spec: Issue 76 法幣下單互動可靠性

- Date: 2026-04-09
- Feature: Issue 76 法幣下單互動可靠性
- Owner: Codex

## Goal
讓法幣下單在 Discord 端先穩定完成 interaction acknowledge，再回填可用的訂單摘要與私訊 fallback，避免使用者因假性互動失敗而誤判下單結果或重複點擊建單。

## Scope

### In Scope
- `ShopSelectMenuHandler` 的法幣下單路徑改為先 `deferReply(true)` 再處理取號與 DM。
- 成功下單後在 ephemeral 回覆中顯示訂單編號、超商代碼、金額與查詢提示。
- 私訊失敗時在原本的 deferred interaction 中保留完整付款備援資訊。
- 對同一 `guildId:userId:productId` 的 in-flight 法幣下單加入互動層防重入保護。

### Out of Scope
- 重用既有未付款訂單或新增資料庫層 dedupe 規則。
- 修改 `FiatOrder` schema、MerchantTradeNo 生成、callback paid 驗證或 fulfillment 邏輯。
- 調整管理員通知流程、商品內容或 ECPay request payload schema。

## Functional Behaviors (BDD)

### Requirement 1: 法幣下單必須先完成 interaction acknowledge
**GIVEN** 使用者在 `/shop` 的法幣商品 select menu 選到一個法幣商品  
**AND** 後續流程需要同步呼叫 ECPay 取號並嘗試送出私訊  
**WHEN** `ShopSelectMenuHandler` 開始處理法幣下單  
**THEN** 系統必須先對 Discord interaction 做 deferred ephemeral reply  
**AND** 後續成功或失敗結果都必須透過 deferred hook 回填，而不是等整個流程結束才第一次回覆

**Requirements**:
- [x] R1.1 法幣下單路徑在進入 ECPay 取號前必須先 `deferReply(true)`。
- [x] R1.2 下單失敗與例外也必須透過 deferred hook 回填可讀錯誤訊息。

### Requirement 2: 使用者必須在 interaction 內拿到可核對的訂單摘要
**GIVEN** 法幣訂單已成功建立  
**AND** 使用者需要訂單編號、超商代碼與查詢提示來完成付款或聯絡管理員  
**WHEN** handler 回填最終 interaction 訊息  
**THEN** ephemeral 訊息必須顯示訂單摘要  
**AND** 即使私訊送出成功，interaction 內仍需保留足以查單的最小必要資訊

**Requirements**:
- [x] R2.1 成功訊息至少要包含商品名稱、訂單編號、超商代碼與金額。
- [x] R2.2 成功訊息要提示使用者保留訂單編號作為後續查詢依據。

### Requirement 3: 私訊失敗時必須提供完整付款備援資訊並阻止互動層重入
**GIVEN** 法幣訂單建立成功  
**AND** DM 開啟或送出可能失敗，且使用者可能在等待期間重複點擊同一商品  
**WHEN** 系統送出私訊或收到同一商品的重複法幣下單互動  
**THEN** DM 失敗時，deferred interaction 必須顯示完整付款備援資訊  
**AND** 同一筆 in-flight 法幣下單不得再觸發第二次取號

**Requirements**:
- [x] R3.1 DM 開啟失敗或送出失敗時，interaction 內必須顯示完整付款摘要與失敗提示。
- [x] R3.2 同一 `guildId:userId:productId` 尚在處理中時，重複互動只回「處理中」提示，不得再次建立新訂單。
- [x] R3.3 防重入 guard 必須在流程完成或失敗後釋放，避免永久卡住後續下單。

## Error and Edge Cases
- [x] 非 guild interaction、空白商品值與商品不存在仍須維持既有錯誤處理。
- [x] ECPay 取號失敗時，不得留下 in-flight guard。
- [x] DM 開啟失敗與 DM 送出失敗都要有可見備援資訊。
- [x] 同一使用者在同一商品連點時，不得因多次 interaction 造成重複建單。
- [x] deferred 回覆後若 handler 內部再拋例外，仍需回填通用錯誤訊息並釋放 guard。

## Clarification Questions
None

## References
- Official docs:
  - `https://docs.discord.com/developers/interactions/receiving-and-responding`
- Related code files:
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/test/java/ltdjms/discord/shop/commands/ShopSelectMenuHandlerTest.java`
  - `src/test/java/ltdjms/discord/shop/services/FiatOrderServiceTest.java`
  - GitHub issue `#76`
