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
- [x] R1.1 新的 runtime gateway 已可在 JDA `awaitReady()` 後由 bootstrap 安全發布到 DI graph；`DiscordCurrencyBot.publishRuntime(...)` 只負責把 ready JDA 發入 injected gateway，並由 `DiscordModuleIntegrationTest` 驗證 singleton wiring。
- [x] R1.2 gateway 介面已收斂為本 repo 需要的最小能力：`findGuild`、`findGuildChannel`、`findThreadChannel`、`selfUserId` 與 ready publication / lookup；未將整個 JDA API 無限制外露。
- [x] R1.3 gateway 尚未 ready 時，會以 `DiscordRuntimeNotReadyException` 明確失敗，`JdaDiscordRuntimeGatewayTest` 已覆蓋 not-ready path。

### Requirement 2: Compatibility bridge 必須是暫時且可控的
**GIVEN** 批次中仍有未遷移的 call sites  
**WHEN** core gateway 先落地  
**THEN** 系統可以暫時保留 compatibility bridge 以維持現行功能  
**AND** 該 bridge 必須被明示為 transitional-only，而不是新的長期 canonical owner

**Requirements**:
- [x] R2.1 `JDAProvider` 已降級為 transitional-only bridge，文件與程式碼都明示新程式不得再以其作為主要邊界。
- [x] R2.2 core spec 已提供可逐步遷移的 adapter surface：正式 DI gateway + 短期 bridge，讓 call site migration 可分批替換。
- [x] R2.3 測試已能在不依賴 global singleton 作為核心驗證邊界的情況下驗證 gateway 行為；`JdaDiscordRuntimeGatewayTest` 與 `DiscordModuleIntegrationTest` 都直接測 injected gateway。

### Requirement 3: 文件與模組邊界必須對齊新抽象
**GIVEN** 專案文件宣稱存在 `discord` abstraction layer  
**WHEN** 此次 core refactor 完成  
**THEN** 文件、module wiring 與 bootstrap 實作必須都把新 gateway 視為唯一正式邊界  
**AND** 不可再讓 `JDAProvider` 與文件中的 abstraction 同時聲稱自己是 owner

**Requirements**:
- [x] R3.1 文件已更新為新 gateway / adapter 模型，並以 `DiscordRuntimeGateway` 作為 runtime access canonical owner。
- [x] R3.2 Dagger wiring、bootstrap、測試 helper 已對齊新抽象：`DiscordModule` 提供 gateway、`AppComponent` 直接暴露 gateway、測試 component 以 singleton scope 取得同一實例。
- [x] R3.3 文件已寫明 bridge 仍保留的刪除條件與存活範圍：僅作 transitional bridge，待 call site migration 完成後刪除。

## Error and Edge Cases
- [x] JDA 尚未 ready 時，gateway 會丟出 `DiscordRuntimeNotReadyException`，不會回傳模糊 null。
- [x] 多執行緒下發布/讀取 runtime handle 時不會暴露部分初始化狀態；`AtomicReference` + single publish guard 已封住重複發布與可見性問題。
- [x] 不可把整個 JDA 物件無限制下放到所有模組：目前只在 gateway 邊界保留 `requireReadyJda()`，其餘業務面使用窄介面查詢。
- [x] bridge 存在期間仍以文件與程式碼註解限制其用途，不把它視為 canonical owner。
- [x] 核心 gateway 驗證測試已不需要 `setJda()` / `clear()` 來驅動；bridge 只保留給尚未遷移的既有 call sites 與少量相容測試。

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
