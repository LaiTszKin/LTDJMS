# Spec: 護航購買通知

- Date: 2026-04-28
- Feature: 護航購買通知
- Owner: [To be filled]

## Goal
在用戶購買有接入護航服務的商品時，系統即時透過 Discord DM 通知管理人員進行派單；並在付款成功後通知買家訂單正在等待處理。

## Scope

### In Scope
- 貨幣購買護航商品後，DM 通知有權限發派護航訂單的管理人員（客戶、訂單詳情、付款狀態）
- 法幣付款完成後，DM 通知有權限發派護航訂單的管理人員（客戶、訂單詳情、付款狀態）
- 貨幣購買護航商品成功後，DM 通知買家「訂單正在等待處理」
- 法幣付款成功後，DM 通知買家「訂單正在等待處理」
- 通知訊息的內容格式（需包含客戶資訊、訂單詳情、付款狀態）

### Out of Scope
- 護航者（司機端）的通知（已有實作）
- 派單面板 (`/dispatch-panel`) 的互動邏輯或 UI 變更
- 法幣付款流程中的買家付款成功通知（已有實作：`FiatOrderBuyerNotificationService`），僅擴充護航相關訊息
- 訂單狀態流轉過程中的通知（確認、完成、售後等）
- 通知歷史紀錄或持久化追蹤欄位

## Functional Behaviors (BDD)

### Requirement 1: 管理員接收貨幣購買護航商品通知

**GIVEN** 用戶在伺服器中選擇以貨幣購買商品
**AND** 該商品已啟用自動護航開單 (`shouldAutoCreateEscortOrder = true`)
**WHEN** 貨幣購買成功
**AND** 護航交接單 (`EscortDispatchOrder`) 自動建立成功
**THEN** 系統應向該伺服器內所有具備 ADMINISTRATOR 權限的成員（排除 bot 自身）發送 DM 通知
**AND** 通知訊息內含：
  - 客戶資訊（買家 mention）
  - 訂單詳情（商品名稱、護航選項、護航訂單編號、來源訂單編號）
  - 付款狀態（已付款、價格快照）

**Requirements**:
- [x] R1.1 貨幣購買完成後，若商品有護航服務，管理員應收到 DM
- [x] R1.2 DM 內容應包含買家、商品名稱、護航選項、訂單編號、付款金額與狀態
- [x] R1.3 管理員通知不應發送給 bot 自身
- [x] R1.4 個別管理員 DM 失敗不應影響其他管理員通知

### Requirement 2: 管理員接收法幣付款護航商品通知

**GIVEN** 用戶已完成法幣商品付款
**AND** 該商品已啟用自動護航開單 (`shouldAutoCreateEscortOrder = true`)
**WHEN** 法幣付款處理流程 (`FiatOrderPostPaymentWorker`) 執行
**AND** 護航交接單 (`EscortDispatchOrder`) 自動建立成功
**THEN** 系統應向該伺服器內所有具備 ADMINISTRATOR 權限的成員發送 DM 通知
**AND** 通知訊息應同 R1.2 格式，含付款狀態（法幣付款、金額）

**Requirements**:
- [x] R2.1 法幣付款完成後，若商品有護航服務，管理員應收到 DM
- [x] R2.2 DM 內容同 R1.2 規範（含法幣付款狀態）
- [x] R2.3 管理員通知失敗時應釋放 claim 以利後續重試

### Requirement 3: 買家接收貨幣購買護航商品通知

**GIVEN** 用戶選擇以貨幣購買啟用護航服務的商品
**WHEN** 貨幣購買成功
**AND** 護航交接單自動建立成功
**THEN** 系統應 DM 買家，通知護航訂單已建立並等待處理
**AND** DM 內容應包含：
  - 商品名稱
  - 護航訂單編號
  - 等待處理說明

**Requirements**:
- [x] R3.1 貨幣購買護航商品成功後，買家應收到 DM 通知
- [x] R3.2 DM 應明確說明護航訂單已建立且正在等待處理

### Requirement 4: 買家接收法幣付款護航商品通知

**GIVEN** 用戶已付款法幣商品
**AND** 該商品已啟用護航服務
**WHEN** 法幣付款處理中護航交接單自動建立成功
**THEN** 系統應 DM 買家，通知護航訂單已建立並等待處理
**AND** DM 內容應同 R3.2（商品名稱、護航訂單編號、等待處理說明）

**Requirements**:
- [x] R4.1 法幣付款處理中護航交接建立成功後，買家應收到 DM 通知
- [x] R4.2 DM 應明確說明護航訂單已建立且正在等待處理

## Error and Edge Cases

- [x] 管理員 DM 私訊關閉時應優雅處理（catch failure callback），不影響流程 — 已有 `sendAdminNotification` try-catch 與 failure callback
- [x] 買家 DM 私訊關閉時應優雅處理，僅記錄 warn log — UT-06 驗證
- [x] `EscortDispatchOrder` 物件為 null 時不應發送通知（guard clause）— 新 service 與既有 service 皆有 null guard
- [x] 商品未啟用護航服務時不應發送任何護航相關通知 — 既有 `shouldAutoCreateEscortOrder()` 檢查
- [x] 法幣付款處理中 escort handoff 失敗時，已有實作會釋放 claim 重試，不另做通知
- [x] `DiscordRuntimeGateway.retrieveUserById()` 非同步失敗時應記錄 warn log — UT-06 驗證 failure callback 不拋錯
- [x] 貨幣購買流程中 handoff 失敗時（`Result.err`），已有實作會回傳錯誤訊息給買家，不另做通知

## Clarification Questions

None — 需求已明確，且現有程式碼結構提供了足夠的實作參考。

## References
- Official docs:
  - JDA 5.2.2: `User.openPrivateChannel()`, `PrivateChannel.sendMessage()`
  - Dagger 2.52: dependency injection pattern
- Related code files:
  - `shop/services/ShopAdminNotificationService.java` — 現有管理員通知服務
  - `shop/services/FiatOrderBuyerNotificationService.java` — 現有買家通知服務
  - `shop/commands/ShopSelectMenuHandler.java` — 貨幣購買觸發點
  - `shop/services/FiatOrderPostPaymentWorker.java` — 法幣付款處理觸發點
  - `dispatch/services/EscortDispatchHandoffService.java` — 護航交接建立服務
  - `dispatch/domain/EscortDispatchOrder.java` — 護航訂單實體
