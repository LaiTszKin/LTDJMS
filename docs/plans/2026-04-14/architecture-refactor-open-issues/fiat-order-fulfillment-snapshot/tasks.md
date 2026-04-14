# Tasks: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot

## **Task 1: 定義 fiat order 履約快照欄位**

對應 `R1.x`，核心目標是讓 `fiat_order` 在建單時就擁有完整履約契約。

- 1. [ ] 擴充 domain / schema / repository
  - 1.1 [ ] 定義 reward、escort 與 buyer-facing display 所需的最小快照欄位
  - 1.2 [ ] 規範建單前驗證，避免產出不可履約的 pending order
  - 1.3 [ ] 規劃舊資料與 migration/backfill 策略

## **Task 2: 讓 post-payment worker 改為 replay snapshot**

對應 `R2.x`，核心目標是從 live product row 解除履約耦合。

- 2. [ ] 重構 worker 與履約服務
  - 2.1 [ ] 移除以 `ProductService` 查詢 live product 作為履約決策來源的做法
  - 2.2 [ ] 對齊 reward grant 與 escort handoff 所需輸入
  - 2.3 [ ] 保持 buyer/admin/reward/escort side effect 的 claim / idempotency 規則

## **Task 3: 補齊測試與文件**

對應 `R3.x`，核心目標是固定快照 owner 邊界，避免未來又回到 live product。

- 3. [ ] 建立回歸覆蓋
  - 3.1 [ ] 新增 product edited / deleted after order creation 的付款履約測試
  - 3.2 [ ] 更新文件與 architecture 說明
  - 3.3 [ ] 確認命名與欄位語意不會被誤認成 cache 而非 contract snapshot
