# Tasks: Fiat order expiry lifecycle

- Date: 2026-04-14
- Feature: Fiat order expiry lifecycle

## **Task 1: 擴充 fiat order 狀態模型與到期欄位**

對應 `R1.x`，核心目標是讓未付款逾期變成可查詢的 terminal state，而不是隱性停止掃描。

- 1. [ ] 調整 domain / schema / repository
  - 1.1 [ ] 新增 terminal status 與 expire-at / cancelled-at 等必要欄位
  - 1.2 [ ] 定義 terminal reason 與營運查詢所需的最小欄位集
  - 1.3 [ ] 規劃歷史資料與既有 pending order 的 backfill/fallback

## **Task 2: 重構 reconciliation / expiry transition**

對應 `R2.x`，核心目標是讓 scheduler 明確區分「仍可付款」與「已逾期」訂單。

- 2. [ ] 建立到期轉態機制
  - 2.1 [ ] 更新 selection query，只掃描仍在有效期內的 pending order
  - 2.2 [ ] 新增 expiry transition path 與 callback/query race 規則
  - 2.3 [ ] 驗證 reconciliation 與 expiry 不會留下雙重真相

## **Task 3: 更新文案、查詢與測試矩陣**

對應 `R3.x`，核心目標是讓 buyer-facing 文案與維運文件都能反映真實生命週期。

- 3. [ ] 對齊文件與測試
  - 3.1 [ ] 更新建單私訊、feature/config/architecture 文件
  - 3.2 [ ] 補齊 pending / paid / expired 顯示與查詢測試
  - 3.3 [ ] 補齊 near-deadline paid / repeated scheduler / historical row edge case coverage
