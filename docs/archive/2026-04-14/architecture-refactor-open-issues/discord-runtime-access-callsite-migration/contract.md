# Contract: Discord runtime access callsite migration

- Date: 2026-04-14
- Feature: Discord runtime access callsite migration
- Change Name: discord-runtime-access-callsite-migration

## Purpose
call site migration 直接依賴 core spec 提供的 gateway 抽象，但實際能力仍包裝 JDA runtime；本文件保留最小的官方依據，避免遷移時把過寬 JDA 表面積重新外露到業務模組。

## Usage Rule
- 只關注業務模組真正需要的 JDA 能力，而非完整 API surface。
- 遷移目標是縮小依賴面，不是把 `JDAProvider.getJda()` 換成 `injected JDA`。
- owned modules 的實際依賴邊界是注入式 `DiscordRuntimeGateway` / 窄 adapter；`JDAProvider` 只可作為 bootstrap / compatibility bridge，不得回流成業務模組的 canonical 入口。

## Dependency Records

### Dependency 1: JDA runtime object model
- Type: `library`
- Version / Scope: `Not fixed`
- Official Source: `https://docs.jda.wiki/net/dv8tion/jda/api/JDA.html`
- Why It Matters: call site migration 需要識別哪些能力該留在窄 adapter，哪些不應直接散播到業務模組
- Invocation Surface:
  - Entry points: guild/user/channel/thread lookup、self user access、DM open/send 等常用能力
  - Call pattern: `in-process async`
  - Required inputs: runtime-ready JDA、Discord IDs
  - Expected outputs: guild/member/user/channel handles、非同步 Discord side effects
- Constraints:
  - Supported behavior: Discord 查詢與訊息發送多為 async / callback-based
  - Limits: 業務模組不應依賴比需求更大的 JDA surface
  - Compatibility: adapter 需保留目前業務語意與錯誤處理
  - Security / access: 受 bot 權限與 Discord 資源存在性限制
- Failure Contract:
  - Error modes: guild/channel/user 不存在、DM 開啟失敗、權限不足、runtime not ready
  - Caller obligations: 透過 adapter 明確處理錯誤而不是直接向外散播全域 state
  - Forbidden assumptions: 不可假設 injected gateway 等於允許任何模組直接拿到完整 JDA
- Verification Plan:
  - Spec mapping: `R1.1-R3.3`
  - Design mapping: `Component Changes`, `Validation Plan`
  - Planned coverage: `UT-fake-runtime-gateway`, `UT-no-jdaprovider-direct-call`, targeted integration tests, `mvn test`
  - Evidence notes: 官方 JDA 類型只是 runtime object model；本地需自行決定更窄的 dependency boundary
