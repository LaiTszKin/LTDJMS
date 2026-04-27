# Tasks: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration

## **Task 1: 遷移 aiagent call sites**

對應 `R1.1-R2.x`，核心目標是讓 AI agent 相關服務與監聽器先擺脫 static JDA access。

- 1. [x] 遷移 `aiagent` 模組
  - 1.1 [x] 盤點並改寫 services/listeners/tools 的 runtime 依賴
  - 1.2 [x] 將測試 fixture 改為 fake gateway
  - 1.3 [x] 驗證 thread history / tool execution / config service 錯誤語意維持一致

## **Task 2: 遷移 aichat 與 shop call sites**

對應 `R1.2-R1.3`，核心目標是讓 AI chat 與訂單通知都改走正式 gateway。

- 2. [x] 遷移 `aichat` / `shop`
  - 2.1 [x] 改寫聊天服務與通知服務的 runtime 依賴
  - 2.2 [x] 補齊 guild not found / DM failure / not-ready 測試
  - 2.3 [x] 確認業務錯誤訊息與 side effect semantics 不變

## **Task 3: 補上 guardrail 與文件**

對應 `R3.x`，核心目標是讓 direct static dependency 不再回流。

- 3. [x] 收斂遷移模式
  - 3.1 [x] 新增搜尋/測試 guardrail，限制新增 `JDAProvider.getJda()` call site
  - 3.2 [x] 更新測試 helper / 開發文件，示範 fake gateway 模式
  - 3.3 [x] 記錄尚未遷移模組與 bridge 存續邊界
