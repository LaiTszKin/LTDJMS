# Tasks: AI memory canonical source

- Date: 2026-04-14
- Feature: AI memory canonical source

## **Task 1: 決定 canonical runtime owner**

對應 `R1.x`，核心目標是收斂到單一 runtime conversation memory 路徑。

- 1. [x] 明確選擇 canonical 方向
  - 1.1 [x] 比較 `SimplifiedChatMemoryProvider` 與舊 persistence path 的實際接線與維護成本
  - 1.2 [x] 定義保留、改名或刪除舊 persistence artifacts 的策略
  - 1.3 [x] 對齊 `AIAgentModule` wiring 與最終設計

## **Task 2: 分離 runtime memory 與 audit/diagnostic 資料**

對應 `R2.x`，核心目標是讓非 runtime 資料不再冒充 conversation memory。

- 2. [x] 重構命名與責任
  - 2.1 [x] 定義工具調用歷史、thread history 與可選 audit storage 的責任邊界
  - 2.2 [x] 規劃 conversation tables 的 drop/rename/repurpose migration
  - 2.3 [x] 收斂 deprecated provider/repository 的存留策略

## **Task 3: 文件與測試收斂**

對應 `R3.x`，核心目標是避免未來再次產生平行 canonical 路徑。

- 3. [x] 更新文件與驗證矩陣
  - 3.1 [x] 重寫 `docs/modules/aiagent.md` 與相關說明
  - 3.2 [x] 更新單元測試，只保護選定的 runtime path
  - 3.3 [x] 增加 migration / deprecation / restart semantics 的回歸檢查
