# Tasks: 護航購買通知

- Date: 2026-04-28
- Feature: 護航購買通知

## **Task 1: 建立買家護航訂單通知服務**

Purpose: 新增 `EscortOrderBuyerNotificationService`，負責在護航交接單自動建立後 DM 通知買家訂單正在等待處理。
Requirements: R3, R4
Allowed scope: `shop/services/` — 新增檔案
Out of scope: 修改現有 `FiatOrderBuyerNotificationService`；修改任何觸發點

- 1. [ ] 建立 `EscortOrderBuyerNotificationService` 類別
  - Input: `EscortDispatchOrder` 的欄位定義（`customerUserId`、`sourceProductName`、`orderNumber`、`sourceCurrencyPrice`、`sourceFiatPriceTwd`）
  - Touches: 新增 `src/main/java/ltdjms/discord/shop/services/EscortOrderBuyerNotificationService.java`
  - Output: 可注入的 service 類別，包含 `notifyEscortOrderCreated(EscortDispatchOrder)` 方法
  - Done when: 類別編譯通過，可正確發送 Discord DM
  - Verify with: `mvn compile` + 單元測試
  - Unit drift check: UT-01 使用 mock DiscordRuntimeGateway 驗證 `retrieveUserById` 被以正確的 `customerUserId` 呼叫；UT-02 驗證 DM 訊息字串包含預期關鍵字（商品名稱、訂單編號、「等待處理」）
  - Do not: 加入任何持久化追蹤邏輯（`buyerNotifiedAt` 等）

- 1.1 [ ] 實作訊息建構方法 `buildEscortOrderCreatedMessage(EscortDispatchOrder)`
  - Input: 護航訂單實體欄位
  - Touches: `EscortOrderBuyerNotificationService.java`
  - Output: 中文訊息字串
  - Done when: 訊息包含商品名稱、護航訂單編號、付款方式、等待處理說明
  - Verify with: 單元測試驗證訊息格式與關鍵字（UT-02）
  - Unit drift check: UT-02 驗證不同 `sourceType`（CURRENCY_PURCHASE / FIAT_PAYMENT）產生對應的付款方式描述
  - Do not: 使用 Embed 格式（保持純文字以配合現有風格）

- 1.2 [ ] 註冊新 service 到 Dagger 依賴注入模組
  - Input: 專案現有的 Dagger module 檔案
  - Touches: 相關 `@Module` / `@Component` 檔案
  - Output: 可被正確注入的依賴
  - Done when: `mvn compile` 通過，其他類別可注入此 service
  - Verify with: `mvn compile`
  - Unit drift check: N/A — 僅 DI 配置，無業務邏輯

## **Task 2: 整合貨幣購買流程的買家通知**

Purpose: 在 `ShopSelectMenuHandler` 貨幣購買護航商品成功且交接單建立後，呼叫新 service 通知買家。
Requirements: R3
Allowed scope: `shop/commands/ShopSelectMenuHandler.java`
Out of scope: 修改貨幣購買核心邏輯；修改法幣流程

- 2. [ ] 在 `ShopSelectMenuHandler` 中注入 `EscortOrderBuyerNotificationService`
  - Input: Task 1 建立的 service
  - Touches: `ShopSelectMenuHandler.java` — 建構子參數、field
  - Output: 可使用的 service 實例
  - Done when: 編譯通過
  - Verify with: `mvn compile`
  - Unit drift check: N/A — 僅 DI 配置變更

- 2.1 [ ] 在 `onButtonInteraction()` 中貨幣購買成功 + handoff OK 後呼叫買家通知
  - Input: `handoffResult.getValue()`（`EscortDispatchOrder`）
  - Touches: `ShopSelectMenuHandler.java` — `onButtonInteraction()` 方法
  - Output: 買家收到護航訂單 DM
  - Done when: 貨幣購買護航商品後，買家收到「等待處理」DM
  - Verify with: 單元測試驗證呼叫順序（UT-03）+ 手動整合測試
  - Unit drift check: UT-03 驗證 `escortOrderBuyerNotificationService.notifyEscortOrderCreated()` 在 handoff 成功後被呼叫，且傳入正確的 `EscortDispatchOrder`
  - Do not: 在 handoff 失敗時呼叫通知；修改 `successMessage` 格式

## **Task 3: 整合法幣付款流程的買家通知**

Purpose: 在 `FiatOrderPostPaymentWorker` 法幣付款處理中護航交接建立成功後，呼叫新 service 通知買家。
Requirements: R4
Allowed scope: `shop/services/FiatOrderPostPaymentWorker.java`
Out of scope: 修改法幣付款處理的 claim/failure/release 邏輯

- 3. [ ] 在 `FiatOrderPostPaymentWorker` 中注入 `EscortOrderBuyerNotificationService`
  - Input: Task 1 建立的 service
  - Touches: `FiatOrderPostPaymentWorker.java` — 建構子參數、field
  - Output: 可使用的 service 實例
  - Done when: 編譯通過
  - Verify with: `mvn compile`
  - Unit drift check: N/A — 僅 DI 配置變更

- 3.1 [ ] 在 `processSingleOrder()` 中護航 handoff 成功後呼叫買家通知
  - Input: `dispatchOrder`（`EscortDispatchOrder`）
  - Touches: `FiatOrderPostPaymentWorker.java` — `processSingleOrder()` 方法
  - Output: 法幣付款完成後買家收到護航訂單 DM
  - Done when: 法幣付款處理完成護航 handoff 後，買家收到「等待處理」DM
  - Verify with: 單元測試驗證呼叫順序（UT-04）
  - Unit drift check: UT-04 驗證 `escortOrderBuyerNotificationService.notifyEscortOrderCreated()` 在 `adminNotificationService.notifyAdminsOrderCreated()` 之後被正確呼叫
  - Do not: 在 handoff 失敗時呼叫通知；為買家通知加入額外的 claim/release 邏輯（僅最佳努力發送，失敗僅 warn log）

## **Task 4: 強化管理員通知內容含明確付款狀態**

Purpose: 確保 `ShopAdminNotificationService` 的 DM 內容包含顯式的「付款狀態」欄位以滿足規格。
Requirements: R1.2, R2.2
Allowed scope: `shop/services/ShopAdminNotificationService.java` — 僅修改 `buildAdminOrderNotification` 方法
Out of scope: 修改管理員通知觸發邏輯；修改權限判斷邏輯

- 4. [ ] 在管理員通知訊息中增加「付款狀態」行
  - Input: `EscortDispatchOrder` 的 `sourceType`、`sourceCurrencyPrice`、`sourceFiatPriceTwd`
  - Touches: `ShopAdminNotificationService.java` — `buildAdminOrderNotification(Guild, long, EscortDispatchOrder)`
  - Output: 更新後的管理員通知格式
  - Done when: 通知訊息包含 `付款狀態：已付款（NT$xxx / xxx 貨幣）` 行
  - Verify with: 單元測試驗證訊息格式（UT-05）
  - Unit drift check: UT-05 驗證 CURRENCY_PURCHASE 產生「貨幣」付款描述、FIAT_PAYMENT 產生「法幣」付款描述
  - Do not: 修改已有欄位排版；修改 `buildAdminOrderNotification(Guild, long, Product, String, String)` 簽名

## **Task 5: 撰寫測試**

Purpose: 為新功能補充足夠的單元測試涵蓋，確保通知邏輯正確、邊界情況有處理。
Requirements: R1, R2, R3, R4
Allowed scope: `src/test/java/ltdjms/discord/shop/services/` — 測試檔案
Out of scope: 整合測試變更（現有整合測試不受影響）；E2E 測試

- 5. [ ] 為 `EscortOrderBuyerNotificationService` 撰寫單元測試
  - Input: UT-01, UT-02
  - Touches: 新增 `EscortOrderBuyerNotificationServiceTest.java`
  - Output: 測試案例
  - Done when: 所有測試通過
  - Verify with: `mvn test`
  - Unit drift check: N/A — 測試本身就是 drift check
  - Do not: 使用真實 Discord 連線

- 5.1 [ ] 為 `ShopSelectMenuHandler` 撰寫買家通知整合測試
  - Input: UT-03
  - Touches: `ShopSelectMenuHandlerTest.java` — 新增測試方法
  - Output: 測試案例
  - Done when: 測試通過
  - Verify with: `mvn test`
  - Unit drift check: N/A — 測試本身就是 drift check
  - Do not: 修改現有測試案例

- 5.2 [ ] 為 `FiatOrderPostPaymentWorker` 撰寫買家通知順序測試
  - Input: UT-04
  - Touches: `FiatOrderPostPaymentWorkerTest.java` — 新增測試方法
  - Output: 測試案例
  - Done when: 測試通過
  - Verify with: `mvn test`
  - Unit drift check: N/A — 測試本身就是 drift check
  - Do not: 修改現有測試案例

- 5.3 [ ] 為 `ShopAdminNotificationService` 撰寫付款狀態格式測試
  - Input: UT-05
  - Touches: `ShopAdminNotificationServiceTest.java` — 新增測試方法
  - Output: 測試案例
  - Done when: 測試通過
  - Verify with: `mvn test`
  - Unit drift check: N/A — 測試本身就是 drift check
  - Do not: 修改現有測試案例

## Notes
- Task 1 為前置任務，Task 2/3 可平行進行
- Task 4 可與 Task 2/3 平行進行
- Task 5 在所有實作任務完成後執行
- 所有通知採用最佳努力發送（best-effort），失敗僅記錄 warn log，不影響主要業務流程
- 所有新功能應維持與現有專案一致的程式碼風格（SLF4J logging、JDA `queue()` 非同步模式、`DiscordRuntimeGateway` 抽象層）
