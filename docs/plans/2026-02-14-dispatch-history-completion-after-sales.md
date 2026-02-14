# PRD：派單系統歷史與售後流程擴充

- 日期：2026-02-14
- 功能名稱：派單系統歷史與售後流程擴充
- 需求摘要：新增歷史查詢、完單確認與售後分派流程，讓派單可追蹤、可收斂、可閉環。

## Reference

- 參考文件：
  - `docs/modules/dispatch.md`
  - `src/main/java/ltdjms/discord/dispatch/commands/DispatchPanelInteractionHandler.java`
  - `src/main/java/ltdjms/discord/dispatch/services/EscortDispatchOrderService.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java`
  - JDA 官方 Javadocs（ButtonInteractionEvent、EntitySelectMenu、Member OnlineStatus、PrivateChannel）
- 需要修改/新增的檔案：
  - `src/main/resources/db/migration/V016__dispatch_order_completion_and_after_sales.sql`（新增）
  - `src/main/java/ltdjms/discord/dispatch/domain/EscortDispatchOrder.java`
  - `src/main/java/ltdjms/discord/dispatch/domain/EscortDispatchOrderRepository.java`
  - `src/main/java/ltdjms/discord/dispatch/domain/DispatchAfterSalesStaffRepository.java`（新增）
  - `src/main/java/ltdjms/discord/dispatch/persistence/JdbcEscortDispatchOrderRepository.java`
  - `src/main/java/ltdjms/discord/dispatch/persistence/JdbcDispatchAfterSalesStaffRepository.java`（新增）
  - `src/main/java/ltdjms/discord/dispatch/services/EscortDispatchOrderService.java`
  - `src/main/java/ltdjms/discord/dispatch/services/DispatchAfterSalesStaffService.java`（新增）
  - `src/main/java/ltdjms/discord/dispatch/commands/DispatchPanelView.java`
  - `src/main/java/ltdjms/discord/dispatch/commands/DispatchPanelInteractionHandler.java`
  - `src/main/java/ltdjms/discord/panel/services/AdminPanelService.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java`
  - `src/main/java/ltdjms/discord/shared/di/DispatchModule.java`
  - `src/main/java/ltdjms/discord/shared/di/CommandHandlerModule.java`
  - `src/test/java/ltdjms/discord/dispatch/services/EscortDispatchOrderServiceTest.java`
  - `src/test/java/ltdjms/discord/panel/unit/AdminPanelServiceTest.java`
  - `src/test/java/ltdjms/discord/panel/commands/AdminPanelButtonHandlerTest.java`

## 核心需求

- [ ] 系統必須提供「歷史記錄檢查」能力，管理員可查看該 guild 最近建立的護航單（含訂單編號、狀態、護航者、客戶、時間）。
- [ ] 系統必須支援護航者於私訊將 `CONFIRMED` 訂單標記為「已完成待客戶確認」。
- [ ] 系統必須在護航者送出完成後，私訊客戶並提供兩個按鈕：「確認完成」與「申請售後」。
- [ ] 系統必須在客戶未於 24 小時內確認且未申請售後時，將訂單視為完成（狀態自動收斂為完成）。
- [ ] 系統必須支援客戶提交售後申請，並將訂單狀態記錄為售後申請中。
- [ ] 系統必須允許管理員在 `/admin-panel` 設定多位售後人員（guild 級別）。
- [ ] 系統必須在收到售後申請時優先通知「在線售後人員」；若無在線售後，則通知全部售後人員。
- [ ] 系統必須要求售後人員先「接單介入」，且同一售後案件不可由多位售後同時介入。
- [ ] 系統必須在售後人員介入成功後，私訊客戶告知由哪位售後人員接手。
- [ ] 系統必須提供售後人員「完成 / close file」按鈕，僅已接手該案的售後可結案。
- [ ] 系統應在未設定售後人員或私訊失敗時回覆可理解訊息，並保留申請紀錄。

## 業務邏輯流程

1. 管理員查詢派單歷史
   - 觸發條件：管理員在派單面板點擊「歷史記錄」
   - 輸入：guildId、操作者 userId
   - 輸出：最近 N 筆訂單摘要（預設 10 筆）
   - 例外/失敗：查詢失敗時回覆「查詢歷史失敗」

2. 護航者送出完單請求
   - 觸發條件：護航者在 DM 點擊「完成訂單」
   - 輸入：orderNumber、escortUserId
   - 輸出：訂單狀態更新為 `PENDING_CUSTOMER_CONFIRMATION`，並記錄 `completion_requested_at`
   - 例外/失敗：非指定護航者、狀態不合法時拒絕

3. 客戶確認或申請售後
   - 觸發條件：客戶在 DM 點擊「確認完成」或「申請售後」
   - 輸入：orderNumber、customerUserId
   - 輸出：
     - 確認完成：狀態更新為 `COMPLETED`
     - 申請售後：狀態更新為 `AFTER_SALES_REQUESTED` 並進入售後通知流程
   - 例外/失敗：非指定客戶、訂單狀態不符

4. 售後通知分派
   - 觸發條件：訂單進入 `AFTER_SALES_REQUESTED`
   - 輸入：guild 售後人員清單、各售後在線狀態
   - 輸出：
     - 有在線售後：僅通知在線售後
     - 無在線售後：通知全部售後
     - 通知訊息含「接手案件」按鈕
   - 例外/失敗：未設定售後人員時提示管理員需先設定

5. 售後介入（Claim）
   - 觸發條件：售後人員在 DM 點擊「接手案件」
   - 輸入：orderNumber、afterSalesUserId
   - 輸出：
     - 首位成功點擊者：狀態更新為 `AFTER_SALES_IN_PROGRESS`，記錄 `after_sales_assignee_user_id`
     - 系統私訊客戶「由 XXX 售後接手」
   - 例外/失敗：若案件已被接手，後續點擊者收到「此案已由他人接手」

6. 售後結案（Close File）
   - 觸發條件：已接手售後人員點擊「完成 / close file」
   - 輸入：orderNumber、afterSalesUserId
   - 輸出：狀態更新為 `AFTER_SALES_CLOSED`，記錄 `after_sales_closed_at`
   - 例外/失敗：非接手售後不可結案

7. 24 小時超時自動完成
   - 觸發條件：系統在互動/查詢時發現 `completion_requested_at + 24h <= now`
   - 輸入：order 狀態與時間欄位
   - 輸出：對 `PENDING_CUSTOMER_CONFIRMATION` 訂單轉為 `COMPLETED`，記錄 `completed_at`
   - 例外/失敗：更新資料失敗時回傳持久化錯誤

## 需要澄清的問題

- 歷史記錄是否需要第一版就支援條件篩選（依護航者/客戶/狀態），或先固定最近 10 筆？（本次預設：最近 10 筆）
- 「在線售後」是否以 Discord `ONLINE` 狀態為唯一判準，`IDLE/DND` 視為離線？（本次預設：僅 `ONLINE` 視為在線）
- 售後結案後是否要同步通知護航者與客戶？（本次預設：至少通知客戶）

## 測試規劃

### 測試原則與參考

- 單元測試原則：`references/unit-tests.md`
- Property-based 測試原則：`references/property-based-tests.md`
- 整合測試原則：`references/integration-tests.md`
- E2E 測試原則（僅在使用者要求時）：`references/e2e-tests.md`

### 單元測試案例

| ID | 情境 | 期望結果 | 目的 |
| --- | --- | --- | --- |
| UT-01 | 護航者對 `CONFIRMED` 訂單送出完成 | 狀態改為 `PENDING_CUSTOMER_CONFIRMATION` 並帶 `completionRequestedAt` | 驗證完單申請流程 |
| UT-02 | 客戶確認完成（24h 內） | 狀態改為 `COMPLETED` 並記錄完成時間 | 驗證客戶確認流程 |
| UT-03 | 客戶在待確認狀態申請售後 | 狀態改為 `AFTER_SALES_REQUESTED` | 驗證售後申請流程 |
| UT-04 | 有在線售後時觸發通知 | 僅在線售後收到通知 | 驗證優先通知規則 |
| UT-05 | 無在線售後時觸發通知 | 全部售後收到通知 | 驗證 fallback 規則 |
| UT-06 | 兩位售後同時點擊接手 | 僅第一位成功，第二位收到已被接手 | 驗證單一介入鎖定 |
| UT-07 | 成功接手後 | 客戶收到「由某售後接手」DM | 驗證客戶告知 |
| UT-08 | 非接手售後點擊 close file | 被拒絕 | 驗證結案權限 |
| UT-09 | 接手售後點擊 close file | 狀態改為 `AFTER_SALES_CLOSED` | 驗證結案流程 |
| UT-10 | 待確認超過 24h 後觸發查詢/互動 | 訂單自動轉為 `COMPLETED` | 驗證超時自動收斂 |

### Property-based 測試案例

| ID | 性質/不變量 | 生成策略 | 目的 |
| --- | --- | --- | --- |
| PBT-01 | 訂單狀態不可逆回（終態不可回到前序態） | 生成合法/非法狀態轉移事件序列 | 確保狀態機一致性 |
| PBT-02 | 同一案件最多一位售後 assignee | 生成多售後競爭 claim 事件序列 | 確保不可多人介入 |
| PBT-03 | `PENDING_CUSTOMER_CONFIRMATION` 超時必收斂為 `COMPLETED` | 生成不同 `completionRequestedAt` 與 `now` 差值 | 確保超時規則穩定 |

### 整合測試案例（如需）

| ID | 依賴/範圍 | 情境 | 目的 |
| --- | --- | --- | --- |
| IT-01 | PostgreSQL + `JdbcEscortDispatchOrderRepository` | 完整狀態流轉（確認接單→待客戶確認→售後→結案） | 驗證欄位映射與 SQL 更新 |
| IT-02 | PostgreSQL + `JdbcDispatchAfterSalesStaffRepository` | 設定多位售後後讀回 | 驗證 guild 售後清單持久化 |
| IT-03 | Flyway migration | 升版後新增欄位/約束/售後表存在 | 避免部署時 migration 缺漏 |

### E2E 測試案例（僅在使用者要求時）

| ID | 使用者路徑 | 期望結果 | 目的 |
| --- | --- | --- | --- |
| E2E-01 | 不適用（未被要求） | 不適用 | 本次先以單元與整合測試覆蓋核心流程 |
