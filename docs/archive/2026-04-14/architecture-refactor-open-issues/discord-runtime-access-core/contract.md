# Contract: Discord runtime access core

- Date: 2026-04-14
- Feature: Discord runtime access core
- Change Name: discord-runtime-access-core

## Purpose
本規格建立 repo 內部的 Discord runtime access abstraction，但它直接封裝 JDA 官方物件與生命週期；因此需記錄 JDA 的官方抽象與初始化語意，避免本地 gateway 對外暴露錯誤假設。

## Usage Rule
- 只記錄會影響 runtime gateway 設計的 JDA 官方語意。
- 本地 gateway 必須把 JDA 包在 repo 自己的 abstraction 後面，而不是原封不動擴散出去。

## Dependency Records

### Dependency 1: JDA runtime / builder lifecycle
- Type: `library`
- Version / Scope: `Not fixed`
- Official Source: `https://docs.jda.wiki/net/dv8tion/jda/api/JDABuilder.html`
- Why It Matters: JDA 實例是在 bot 啟動時建立與 ready；本地 gateway 必須配合這個生命週期對外暴露能力
- Invocation Surface:
  - Entry points: `JDABuilder`, `JDA`, ready lifecycle
  - Call pattern: `in-process async bootstrap`
  - Required inputs: bot token、gateway intents、cache policy 等設定
  - Expected outputs: 可用的 `JDA` runtime instance
- Constraints:
  - Supported behavior: JDA 需完成初始化後才可安全提供 runtime operations
  - Limits: 本地不得假設任何模組在 DI 初始化時就一定拿得到 ready JDA
  - Compatibility: gateway 要能包裝 JDA ready/not-ready 狀態
  - Security / access: bot token 由既有 env/config 管理
- Failure Contract:
  - Error modes: 初始化失敗、尚未 ready、連線中斷
  - Caller obligations: 對 not-ready 有明確失敗語意，避免靜默 null
  - Forbidden assumptions: 不可假設 process-global static state 是官方推薦的生命周期管理方式
- Verification Plan:
  - Spec mapping: `R1.1-R2.3`
  - Design mapping: `Proposed Architecture`, `Component Changes`
  - Planned coverage: `UT-runtime-gateway-ready-state`, `IT-bootstrap-publishes-runtime`
  - Actual coverage: `mvn -q -Punit-tests test`, `mvn -q -Pintegration-tests test`
  - Evidence notes: 官方 JDA 類型與 builder 文件表明 runtime instance 與 builder/ready lifecycle 分離；實作已在 `DiscordCurrencyBot.awaitReady()` 之後才發布到 injected gateway

## Implementation Notes

- `DiscordRuntimeGateway` 是 repo 內部的最小 runtime boundary，負責 ready publication 與 guild/channel/self user lookup。
- `JdaDiscordRuntimeGateway` 以 `AtomicReference<JDA>` 保存 runtime，確保只會單次發布且 not-ready 狀態會以 `DiscordRuntimeNotReadyException` 明確失敗。
- `JDAProvider` 仍保留為 transitional bridge，但文件與程式碼都標明它不是 canonical owner。
