# Tasks: Escort order handoff

- Date: 2026-04-14
- Feature: Escort order handoff

## **Task 1: 定義 dispatch handoff 契約**

對應 `R1.x-R2.x`，核心目標是讓購買完成能以單一應用服務或 aggregate factory 寫入 dispatch durable state。

- 1. [x] 設計 handoff command / service boundary
  - 1.1 [x] 定義來源訂單、來源商品、escort option 與價格快照的 canonical payload
  - 1.2 [x] 明確規範貨幣購買與法幣付款完成共用的冪等鍵
  - 1.3 [x] 指定 handoff failure 的重試 / 診斷責任歸屬

## **Task 2: 擴充 dispatch aggregate 與資料模型**

對應 `R2.x`，核心目標是讓 `dispatch` 自身能保存與顯示來源購買上下文。

- 2. [x] 新增來源脈絡欄位與讀寫路徑
  - 2.1 [x] 擴充 `EscortDispatchOrder` 與 repository / migration
  - 2.2 [x] 對齊 dispatch UI / query / notification 所需最小欄位集
  - 2.3 [x] 決定既有手動派單資料如何與自動派單欄位共存

## **Task 3: 將通知降級為派生副作用並補齊測試**

對應 `R3.x`，核心目標是避免 admin DM 再被誤當作唯一交接機制。

- 3. [x] 重構購買完成流程與測試
  - 3.1 [x] 在貨幣購買與法幣 post-payment worker 中改為先 handoff，再通知
  - 3.2 [x] 新增 duplicate event、delete-product、DM failure regression coverage
  - 3.3 [x] 更新文件，區分手動 dispatch 與自動 escort handoff 兩種入口
