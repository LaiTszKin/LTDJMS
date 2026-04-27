# Tasks: AI agent channel gating

- Date: 2026-04-14
- Feature: AI agent channel gating

## **Task 1: 重構 mention routing decision**

對應 `R1.x`，核心目標是把 Agent 與一般 AI Chat 的放行邏輯從單一 allowlist 前置判斷拆開。

- 1. [x] 建立清楚的 routing decision abstraction
  - 1.1 [x] 在 `AIChatMentionListener` 中抽出可測的 decision matrix
  - 1.2 [x] 讓 Agent 啟用判斷與一般 AI allowlist 判斷成為兩條可觀測的決策路徑
  - 1.3 [x] 保留既有 streaming / markdown / reasoning 後續流程不受影響

## **Task 2: 對齊設定面與文件**

對應 `R2.x-R3.1`，核心目標是讓 panel、文件與 runtime 使用同一套邊界語意。

- 2. [x] 收斂設定語意與文案
  - 2.1 [x] 檢查 `panel` facade / handler 的說明與回覆文案
  - 2.2 [x] 更新 `docs/features.md`、`docs/architecture.md` 與必要模組文件
  - 2.3 [x] 明確記錄 Agent-only channel 與一般 AI allowlist 的關係

## **Task 3: 補齊測試與回歸矩陣**

對應 `R3.2-R3.3`，核心目標是固定新 routing 行為並防止白名單回歸。

- 3. [x] 新增/調整測試矩陣
  - 3.1 [x] 新增 Agent-only channel、AI-only channel、雙重啟用與雙重拒絕案例
  - 3.2 [x] 驗證 DM / bot / no-mention / thread restriction regression case
  - 3.3 [x] 驗證 logging / observable decision path 不會誤導維運排障
