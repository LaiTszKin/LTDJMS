# Tasks: Admin panel session refresh

- Date: 2026-04-14
- Feature: Admin panel session refresh

## **Task 1: 擴充 session abstraction**

對應 `R1.x`，核心目標是讓 admin panel session 可以按 guild 枚舉並攜帶必要 view metadata。

- 1. [x] 重構 `AdminPanelSessionManager` 與底層抽象
  - 1.1 [x] 決定是擴充 `DiscordSessionManager` 還是以更合適的 panel-specific abstraction 取代
  - 1.2 [x] 保存可刷新當前 view 所需的最小 metadata
  - 1.3 [x] 處理 expired / invalid hook cleanup

## **Task 2: 收斂 event-driven refresh 路徑**

對應 `R2.x`，核心目標是讓 main admin panel 與 product 子頁都透過同一條正式刷新管線更新。

- 2. [x] 實作 guild-wide refresh
  - 2.1 [x] 讓 `AdminPanelUpdateListener` 走正式 traversal path
  - 2.2 [x] 消除或吸收 `AdminProductPanelHandler` 的 ad-hoc product session index
  - 2.3 [x] 避免高頻事件對同一 hook 重複編輯造成噪音

## **Task 3: 補齊測試與文件**

對應 `R3.x`，核心目標是讓文件與測試不再宣稱不存在的即時更新能力。

- 3. [x] 更新驗證矩陣
  - 3.1 [x] 新增多管理員、多 view、expired hook 測試
  - 3.2 [x] 更新 `docs/modules/event-system.md` / panel 相關文件
  - 3.3 [x] 記錄任何仍需使用者重新導覽的例外情況
