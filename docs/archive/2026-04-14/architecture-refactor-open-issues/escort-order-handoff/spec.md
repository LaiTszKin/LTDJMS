# Spec: Escort order handoff

- Date: 2026-04-14
- Feature: Escort order handoff
- Owner: Codex

## Goal
讓商品或法幣付款觸發的護航需求真正落成 dispatch 模組中的 durable 工作項，而不是停在一次性的管理員私訊提醒。

## Scope

### In Scope
- 為 `auto_create_escort_order` 商品建立正式的 dispatch handoff 模型
- 擴充 `EscortDispatchOrder` 或等價 aggregate，使其承載來源商品 / 訂單 / escort option / 價格快照脈絡
- 重構貨幣購買與法幣付款完成流程，讓管理員通知從 dispatch record 衍生

### Out of Scope
- 重做 `/dispatch-panel` 的操作體驗、分派規則或售後流程
- 重新設計商品管理面板全部欄位
- 將護航分派改成自動指派護航者

## Functional Behaviors (BDD)

### Requirement 1: 購買完成必須產生 durable dispatch work item
**GIVEN** 商品啟用了 `auto_create_escort_order`  
**AND** 商品已配置有效的 `escort_option_code`  
**WHEN** 使用者完成貨幣購買或法幣訂單付款成功  
**THEN** 系統必須建立一筆可持久化的 dispatch-domain 訂單或等價工作項  
**AND** 該工作項必須攜帶來源訂單與 escort option 的必要脈絡

**Requirements**:
- [x] R1.1 貨幣購買與法幣付款完成都必須透過同一個 handoff boundary 寫入 dispatch durable state
  - 完成證據：`EscortDispatchHandoffService` 成為共同交接邊界，`ShopSelectMenuHandler` 與 `FiatOrderPostPaymentWorker` 都先 handoff 再通知。
- [x] R1.2 handoff 成功前不得把「護航已交接」視為完成
  - 完成證據：admin DM 只是衍生副作用；若通知失敗，dispatch record 仍保留且 fiat order 不會被標記為完成。
- [x] R1.3 handoff 必須具備冪等保護，避免重複 callback / worker 重跑時建立重複 dispatch 訂單
  - 完成證據：以 `sourceType + sourceReference` 查重，整合測試覆蓋重複來源參考回傳同一筆 dispatch order。

### Requirement 2: Dispatch aggregate 必須能表示來源購買脈絡
**GIVEN** dispatch 模組要負責後續護航工作流  
**WHEN** 它接收來自商品/付款流程的自動開單請求  
**THEN** aggregate 必須保存足以追溯來源商品、來源訂單、escort option 與價格快照的欄位  
**AND** 不可再要求管理員從 DM 文案手動重建上下文

**Requirements**:
- [x] R2.1 `EscortDispatchOrder` 或等價模型必須保存 source order reference、source product reference、escort option snapshot 與建立來源
  - 完成證據：新增 `SourceType`、`sourceReference`、`sourceProductId`、`sourceProductName`、`sourceCurrencyPrice`、`sourceFiatPriceTwd`、`sourceEscortOptionCode`。
- [x] R2.2 後續 dispatch UI / DM /審計資訊應從 durable record 讀取，而不是重新拼裝一次性訊息
  - 完成證據：`DispatchPanelMessageFactory` 與 `ShopAdminNotificationService` 直接讀取 dispatch durable record 產生命文。
- [x] R2.3 若來源商品後續被編輯或刪除，既有 dispatch 訂單的執行上下文不得消失
  - 完成證據：整合測試覆蓋商品刪除後仍可從 dispatch record 讀回來源快照。

### Requirement 3: 管理員通知必須降級為衍生副作用
**GIVEN** 系統仍需要提醒管理員有新的護航工作  
**WHEN** dispatch handoff 完成  
**THEN** 通知必須從新建立的 dispatch 訂單衍生  
**AND** 不可再讓「有收到 DM」成為唯一代表開單成功的信號

**Requirements**:
- [x] R3.1 admin DM / panel 提醒只可在 durable handoff 成功後送出
  - 完成證據：`ShopSelectMenuHandler`、`FiatOrderPostPaymentWorker` 都在 handoff 成功後才呼叫通知服務。
- [x] R3.2 handoff 失敗時必須保留可重試或可診斷狀態，而不是只留下一次 warn log
  - 完成證據：整合測試驗證通知失敗時 dispatch record 已落庫且 fiat order 保持可重試。
- [x] R3.3 文件必須明確把 `/dispatch-panel` 的手動開單與商品觸發的自動開單區分為兩種入口、同一個 aggregate
  - 完成證據：dispatch 模組文件已保留手動入口說明，shop 文件補上自動 handoff 與 canonical aggregate 的說明。

## Error and Edge Cases
- [ ] 同一筆購買事件重放時不得建立重複 dispatch 訂單
- [ ] 來源商品被刪除、escort option 失效或價格更新後，既有 dispatch record 仍需可操作
- [ ] admin DM 發送失敗不得回滾已建立的 dispatch record
- [ ] dispatch aggregate 若尚未能承載來源欄位，不得先以暫時字串欄位或 log 作為長期替代
- [ ] 貨幣購買與法幣購買的 handoff 必須共用相同資料語意，避免兩套 record shape 漂移

## Clarification Questions
None

## References
- Official docs:
  - None（此次變更聚焦 repo 內部 `product` / `shop` / `dispatch` 邊界重構）
- Related code files:
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderPostPaymentWorker.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/ShopAdminNotificationService.java`
  - `src/main/java/ltdjms/discord/dispatch/domain/EscortDispatchOrder.java`
  - `src/main/java/ltdjms/discord/product/domain/Product.java`
