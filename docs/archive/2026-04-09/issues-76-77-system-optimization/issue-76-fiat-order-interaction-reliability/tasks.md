# Tasks: Issue 76 法幣下單互動可靠性

- Date: 2026-04-09
- Feature: Issue 76 法幣下單互動可靠性

## **Task 1: 收斂 deferred interaction 流程**

對應 `R1.1-R1.2`，核心目標是把法幣下單改成先 acknowledge、再用 hook 回填結果。

- 1. [x] 重構 `ShopSelectMenuHandler` 的法幣下單入口
  - 1.1 [x] 在法幣下單開始時先 `deferReply(true)`，再執行 ECPay 建單與 DM 流程
  - 1.2 [x] 把失敗與例外訊息改為透過 deferred hook 回填

## **Task 2: 補齊訂單摘要與私訊 fallback**

對應 `R2.1-R2.2`、`R3.1`，核心目標是讓使用者不依賴私訊也能取得付款與查詢資訊。

- 2. [x] 整理 interaction 成功／失敗訊息內容
  - 2.1 [x] 成功時在 ephemeral 訊息顯示訂單摘要與查詢提示
  - 2.2 [x] DM 開啟或送出失敗時，改為在 interaction 顯示完整付款備援資訊

## **Task 3: 加入互動層防重入與回歸測試**

對應 `R3.2-R3.3`，核心目標是在不動資料庫 dedupe 規則的前提下，阻止同一互動窗口內的重複建單。

- 3. [x] 擴充 handler 與相關服務測試
  - 3.1 [x] 覆蓋 deferred reply、成功摘要、DM fallback 與處理中提示
  - 3.2 [x] 驗證 in-flight guard 會在成功與錯誤後釋放

## Notes
- 本 spec 不新增 repository dedupe 或重用既有未付款訂單規則；若之後要做資料層 dedupe，需另開 spec。
- property-based coverage 預計標記 `N/A`，因為這次變更主要是 interaction 協調與訊息組裝，沒有新增可生成測試空間的業務規則。
