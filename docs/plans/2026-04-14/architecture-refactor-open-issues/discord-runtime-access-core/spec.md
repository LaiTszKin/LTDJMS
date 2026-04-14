# Spec: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core
- Owner: Codex

## Goal
把 live JDA 實例的取得方式收斂回正式 DI / abstraction 邊界，讓 `shared` / `discord` 成為 Discord runtime access 的唯一正式入口，而不是 `JDAProvider` 這個 process-global static singleton。

## Scope

### In Scope
- 設計並注入一個正式的 Discord runtime gateway / handle abstraction
- 調整 bootstrap / Dagger wiring，使 JDA 初始化後可安全發布到注入圖
- 為尚未遷移的模組保留短期 compatibility bridge，但不再允許新程式直接依賴 static provider

### Out of Scope
- 一次性遷移所有 call sites（由 `discord-runtime-access-callsite-migration` 承接）
- 更換 JDA 或 Discord 平台 SDK
- 重做 bot 啟動流程整體架構

## Functional Behaviors (BDD)

### Requirement 1: Discord runtime access 必須有正式注入邊界
**GIVEN** JDA 只有在 bot 啟動完成後才可用  
**WHEN** 業務模組需要查詢 guild、user、channel 或 bot identity  
**THEN** 它們必須透過正式注入的 Discord runtime gateway 取得這些能力  
**AND** 不可再把 mutable static global state 當成主要邊界

**Requirements**:
- [ ] R1.1 新的 runtime gateway 必須可在 JDA ready 之後由 bootstrap 安全發布到 DI graph
- [ ] R1.2 gateway 介面必須只暴露本 repo 真正需要的最小能力，而不是把整個 JDA API 無限制外露
- [ ] R1.3 gateway 尚未 ready 時，錯誤語意必須明確且可測

### Requirement 2: Compatibility bridge 必須是暫時且可控的
**GIVEN** 批次中仍有未遷移的 call sites  
**WHEN** core gateway 先落地  
**THEN** 系統可以暫時保留 compatibility bridge 以維持現行功能  
**AND** 該 bridge 必須被明示為 transitional-only，而不是新的長期 canonical owner

**Requirements**:
- [ ] R2.1 `JDAProvider` 若暫時保留，必須被降級為 bridge，且新程式碼不得新增依賴
- [ ] R2.2 core spec 需提供讓 call site migration 可逐步遷移的 adapter surface
- [ ] R2.3 測試必須能在不碰 global singleton 的情況下驗證 gateway 行為

### Requirement 3: 文件與模組邊界必須對齊新抽象
**GIVEN** 專案文件宣稱存在 `discord` abstraction layer  
**WHEN** 此次 core refactor 完成  
**THEN** 文件、module wiring 與 bootstrap 實作必須都把新 gateway 視為唯一正式邊界  
**AND** 不可再讓 `JDAProvider` 與文件中的 abstraction 同時聲稱自己是 owner

**Requirements**:
- [ ] R3.1 文件必須更新為新 gateway / adapter 模型
- [ ] R3.2 Dagger wiring、bootstrap、測試 helper 必須對齊新抽象
- [ ] R3.3 若 bridge 尚保留，文件必須寫明其刪除條件與存活範圍

## Error and Edge Cases
- [ ] JDA 尚未 ready 時，gateway 不得回傳模糊 null 或迫使呼叫端猜測狀態
- [ ] 多執行緒下發布/讀取 runtime handle 時不得暴露部分初始化狀態
- [ ] 不可把整個 JDA 物件無限制下放到所有模組，否則只是把 static 換成 injected global
- [ ] bridge 存在期間要防止新模組繼續新增 direct static dependency
- [ ] 測試不應再需要 `setJda()` / `clear()` 才能跑核心抽象層驗證

## Clarification Questions
None

## References
- Official docs:
  - https://docs.jda.wiki/net/dv8tion/jda/api/JDA.html
  - https://docs.jda.wiki/net/dv8tion/jda/api/JDABuilder.html
- Related code files:
  - `src/main/java/ltdjms/discord/shared/di/JDAProvider.java`
  - `src/main/java/ltdjms/discord/currency/bot/DiscordCurrencyBot.java`
  - `docs/modules/discord-api-abstraction.md`
