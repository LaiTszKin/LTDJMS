# 模組說明：派單護航系統

本文件介紹 `dispatch/` 模組的實作，涵蓋 `/dispatch-panel` 指令、互動面板流程、訂單狀態流轉與資料庫結構。

## Documentation Delta

- 補充 2026-03-10 的互動層重構：`DispatchPanelInteractionHandler` 已將歷史/DM/售後訊息組裝抽到 `DispatchPanelMessageFactory`，handler 專注於流程協調與狀態流轉。
- 補充 2026-04-27 的面板流程調整：`/dispatch-panel` 先選擇「開單」或「派單」；開單建立待派發護航訂單，派單則把既有待派單訂單指派給護航者並送出 DM 確認。

## 1. 功能概觀

派單護航系統提供一條「建立待派發護航訂單 → 管理員派發給護航者 → 護航者私訊確認 → 客戶收到確認通知」的流程，並同時作為商品/付款完成後自動護航交接的唯一 durable aggregate。

主要能力：

- 管理員透過 `/dispatch-panel` 先選擇「開單」或「派單」
- 開單流程可選擇客戶與任一內建護航品類，建立待派發訂單
- 商品 / 付款完成後的自動護航交接也會建立待派發訂單
- 派單流程會列出現有待派發訂單，管理員選擇訂單與護航者後派發
- 系統私訊護航者，附上「確認接單」按鈕
- 護航者確認後，訂單狀態改為 `CONFIRMED`，並通知護航者與客戶

## 2. 主要程式結構

### 2.1 指令與互動層（commands）

- `DispatchPanelCommandHandler`
  - 處理 `/dispatch-panel` 指令
  - 僅允許 guild 內使用
  - 回覆 ephemeral 派單面板

- `DispatchPanelInteractionHandler`
  - 處理 String Select、Entity Select 與按鈕互動
  - 維護每位管理員在每個 guild 的面板暫存狀態（`sessionStates`）
  - 開單前驗證：
    - 客戶與護航品類是否完整
    - 客戶是否仍在 guild
  - 派單前驗證：
    - 待派發訂單與護航者是否完整
    - 護航者與客戶不可同一人
    - 成員是否仍在 guild
  - 處理護航者 DM 中的「確認接單」按鈕
  - 專注於流程協調、狀態流轉與通知送出，不再直接負責各類 embed 文案組裝

- `DispatchPanelMessageFactory`
  - 組裝歷史訂單、護航者 DM、客戶確認、售後通知與來源摘要等訊息 embed
  - 將顯示文案與狀態對應集中管理，降低 handler 的責任範圍

- `DispatchPanelView`
  - 組裝模式選擇、開單表單、派單表單、Embed 與元件
  - 未完成必要選擇前，開單 / 派單按鈕為停用狀態

### 2.2 領域層（domain）

- `EscortDispatchOrder`
  - 訂單實體（record）
  - 狀態：
    - `PENDING_CONFIRMATION`
    - `CONFIRMED`
  - 不變條件：
    - `orderNumber` 不可空白、長度不可超過 32
    - `escortUserId` 與 `customerUserId` 不可相同
    - `CONFIRMED` 狀態必須有 `confirmedAt`
  - `escortUserId = 0` 代表訂單尚未派發給護航者
  - 自動護航單另保存 `sourceType`、`sourceReference`、`sourceProductId`、`sourceProductName`、`sourceCurrencyPrice`、`sourceFiatPriceTwd`、`sourceEscortOptionCode`
  - 手動開單可在 `MANUAL` 來源上保存 `sourceEscortOptionCode` 作為護航品類

- `EscortDispatchOrderRepository`
  - 訂單儲存介面，提供 `save`、`update`、`findByOrderNumber`、`findPendingAssignmentByGuildId`、`assignEscort`、`existsByOrderNumber`

### 2.3 服務層（services）

- `EscortDispatchOrderService`
  - 建立訂單：`createOrder(...)`
  - 手動開單：`createManualOpenOrder(...)`
  - 派發待派發訂單：`assignPendingOrder(...)`
  - 確認訂單：`confirmOrder(orderNumber, confirmerUserId)`
  - 訂單編號唯一性保證：最多重試 20 次

- `EscortDispatchHandoffService`
  - 把貨幣購買或法幣付款完成的護航需求寫入 dispatch durable state
  - 以 `sourceType + sourceReference` 做冪等查重
  - handoff 成功後才交由通知服務發送 admin DM / panel 提醒

- `EscortDispatchOrderNumberGenerator`
  - 訂單編號格式：`ESC-YYYYMMDD-XXXXXX`
  - 後綴使用 `SecureRandom` 與可讀字元集（排除易混淆字元）

### 2.4 持久化層（persistence）

- `JdbcEscortDispatchOrderRepository`
  - JDBC 實作 `EscortDispatchOrderRepository`
  - 以 `order_number` 查詢與唯一性檢查
  - 可查詢 `escort_user_id = 0` 且 `PENDING_CONFIRMATION` 的待派發訂單
  - 可原子更新待派發訂單的 `assigned_by_user_id` 與 `escort_user_id`

## 3. 流程說明

### 3.1 開單流程

1. 管理員執行 `/dispatch-panel`
2. 面板先顯示「開單 / 派單」模式選單
3. 管理員選擇「開單」
4. 管理員選擇客戶與護航品類，點擊「✅ 建立護航訂單」
5. 系統驗證客戶仍在 guild，並以 `EscortDispatchOrderService.createManualOpenOrder(...)` 建立 `PENDING_CONFIRMATION` 訂單
6. 新訂單的 `escort_user_id = 0`，代表尚未派發給護航者

### 3.2 派單流程

1. 管理員執行 `/dispatch-panel` 並選擇「派單」
2. 面板列出目前 `escort_user_id = 0` 的待派發護航訂單
3. 管理員選擇訂單與護航者，點擊「✅ 派發訂單」
4. `EscortDispatchOrderService.assignPendingOrder(...)` 原子指定護航者
5. 系統私訊護航者，附上「✅ 確認接單」按鈕

> 若護航者私訊失敗：訂單仍會保留為已指派但待確認，系統會回覆管理員需手動通知。

### 3.3 護航者確認流程

1. 護航者在 Bot 私訊中點擊確認按鈕
2. 系統檢查：
   - 訂單是否存在
   - 是否為被指派護航者
   - 訂單是否仍為 `PENDING_CONFIRMATION`
3. 驗證通過後更新為 `CONFIRMED`
4. 系統通知：
   - 更新護航者原私訊為已確認
   - 另行私訊客戶已確認資訊

## 4. 資料庫設計（V014）

Migration：`src/main/resources/db/migration/V014__create_escort_dispatch_order.sql`

### 4.1 資料表

- `escort_dispatch_order`
  - 主鍵：`id`
  - 唯一鍵：`order_number`
  - 欄位：`guild_id`、`assigned_by_user_id`、`escort_role_id`、`customer_role_id`、`escort_user_id`、`customer_user_id`、`status`、`created_at`、`confirmed_at`、`updated_at`

### 4.2 約束與索引

- `status` 檢查約束：僅允許 `PENDING_CONFIRMATION` 或 `CONFIRMED`
- 使用者檢查約束：`escort_user_id <> customer_user_id`
- 索引：
  - `idx_escort_dispatch_order_guild_id`
  - `idx_escort_dispatch_order_status`
  - `idx_escort_dispatch_order_escort_user_id`

### 4.3 觸發器

- `update_escort_dispatch_order_updated_at`
  - `BEFORE UPDATE`
  - 使用共用函式 `update_updated_at_column()` 自動更新 `updated_at`

## 5. DI 與啟動註冊

- `DispatchModule` 提供 repository、service、command handler、interaction handler
- `SlashCommandListener` 註冊 `/dispatch-panel` 指令（管理員限定）
- `DiscordCurrencyBot` 將 `DispatchPanelInteractionHandler` 註冊為事件監聽器

## 6. 測試現況

目前已有：

- `EscortDispatchOrderServiceTest`
  - 建立訂單成功與失敗情境
  - 訂單編號衝突重試
  - 非指定護航者確認失敗
  - 重複確認失敗
  - 成功確認流程
- `EscortDispatchHandoffServiceTest`
  - 自動 handoff 的 snapshot 與冪等查重
- `EscortDispatchHandoffServiceIntegrationTest`
  - 商品刪除後仍可查回來源快照
  - 重複來源參考不會產生重複 dispatch record

## 7. 已知範圍

目前版本聚焦「建立與確認」主流程，尚未包含：

- 訂單取消
- 重新指派
- 訂單列表查詢

## 相關文件

- [Slash Commands 參考](../api/slash-commands.md#dispatch-panel--派單護航面板)
- [計畫文件：派單護航系統](../plans/2026-02-13-escort-dispatch-system.md)
- [系統架構總覽](../architecture/overview.md)
