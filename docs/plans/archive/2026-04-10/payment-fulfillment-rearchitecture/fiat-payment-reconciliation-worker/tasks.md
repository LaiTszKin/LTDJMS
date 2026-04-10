# Tasks: Fiat payment reconciliation worker

- Date: 2026-04-10
- Feature: Fiat payment reconciliation worker

## **Task 1: 拆分 paid callback 與後處理責任**

對應 `R1.x`，核心目標是讓 callback 只做付款真相落庫，不再同步承擔發貨副作用。

- 1. [x] 重構 callback paid transition
  - 1.1 [x] 將 `FiatPaymentCallbackService` 的 paid transition 與 post-payment side effects 拆開
  - 1.2 [x] 保留重複 callback 冪等，避免重複建立後處理工作
  - 1.3 [x] 更新 callback 相關單元測試與回歸測試

## **Task 2: 實作資料庫驅動的 post-payment worker**

對應 `R2.x`，核心目標是讓已付款訂單能在背景流程中可靠重試通知、護航開單與 fulfilled 標記。

- 2. [x] 新增 worker 與必要 schema / repository API
  - 2.1 [x] 新增 order selection / claim / retry 欄位或查詢
  - 2.2 [x] 讓 worker 冪等執行管理員通知、護航開單與 fulfilled 更新
  - 2.3 [x] 將原本 callback 內的同步後處理移轉到 worker

## **Task 3: 補齊 ECPay 補償查單與驗證**

對應 `R3.x`，核心目標是在 callback 遺失時仍能補回 paid transition。

- 3. [x] 新增 pending order reconciliation
  - 3.1 [x] 實作 ECPay 官方查單 API client 或封裝
  - 3.2 [x] 建立定期掃描 `PENDING_PAYMENT` 訂單的補償排程
  - 3.3 [x] 加入 mock/fake 的查單狀態矩陣測試、重複事件測試與整合測試
