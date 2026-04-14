# Tasks: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core

## **Task 1: 設計 Discord runtime gateway**

對應 `R1.x`，核心目標是建立正式注入邊界，取代 static singleton 的 owner 角色。

- 1. [ ] 定義 gateway / handle 抽象
  - 1.1 [ ] 收斂 repo 實際需要的 JDA 能力為最小介面
  - 1.2 [ ] 設計 ready / not-ready 錯誤語意
  - 1.3 [ ] 決定 bootstrap 後如何安全發布到 DI graph

## **Task 2: 落實 compatibility bridge 與測試策略**

對應 `R2.x`，核心目標是讓 core 可先落地，同時允許後續 call site 漸進遷移。

- 2. [ ] 建立 transitional-only bridge
  - 2.1 [ ] 降級 `JDAProvider` 或等價 bridge 的角色
  - 2.2 [ ] 提供 call site migration 可依賴的 adapter surface
  - 2.3 [ ] 建立不依賴 global singleton 的核心測試 fixture

## **Task 3: 更新文件與 Dagger/Bootstrap 說明**

對應 `R3.x`，核心目標是讓文件與實作邊界重新一致。

- 3. [ ] 對齊 docs 與 wiring
  - 3.1 [ ] 更新 discord abstraction 文件與啟動流程說明
  - 3.2 [ ] 補充 bridge 存活條件與刪除計畫
  - 3.3 [ ] 確認 `shared/di` 與 `currency/bot` 的 wiring 描述無歧義
