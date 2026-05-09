# Contract: Issue 76 法幣下單互動可靠性

- Date: 2026-04-09
- Feature: Issue 76 法幣下單互動可靠性
- Change Name: issue-76-fiat-order-interaction-reliability

## Purpose
本次變更直接受 Discord interaction 回應時限與後續 follow-up / edit original 機制約束；若不遵守平台契約，就算後端成功建單，前端仍會顯示互動失敗。

## Dependency Records

### Dependency 1: Discord Interactions
- Type: `platform`
- Version / Scope: `Discord interactions API (current docs)`
- Official Source: `https://docs.discord.com/developers/interactions/receiving-and-responding`
- Why It Matters: 法幣下單是 message component interaction；平台要求先在時限內回應，之後才能用 interaction token 編輯原始回覆或送 follow-up。
- Invocation Surface:
  - Entry points: Discord message component interaction, deferred response, follow-up / edit-original hooks
  - Call pattern: sync acknowledge + async follow-up
  - Required inputs: interaction token、initial response、後續 hook token
  - Expected outputs: 成功 acknowledge、可編輯原始回覆或傳 follow-up、ephemeral user feedback
- Constraints:
  - Supported behavior: initial response 可 deferred，之後可 edit original 或 send follow-up
  - Limits: initial response 需在 3 秒內送出；interaction token 可用 15 分鐘
  - Compatibility: 適用於 slash command、message component 等 interaction 類型
  - Security / access: follow-up 依 interaction token 授權，不可跳過 initial response 直接假設 token 永遠有效
- Failure Contract:
  - Error modes: 超過 3 秒未回應時 token 失效，客戶端顯示互動失敗
  - Caller obligations: 在長流程前先 deferred；後續結果走 hook edit/follow-up
  - Forbidden assumptions: 不可假設同步完成 ECPay + DM 後再第一次回覆仍一定有效
- Verification Plan:
  - Spec mapping: `R1.1-R1.2`, `R2.1-R2.2`, `R3.1-R3.3`
  - Design mapping: `Component 1`, `Sequence / Control Flow`
  - Planned coverage: `UT-76-01~06`
  - Evidence notes: Discord 官方文件明確要求 initial response 在 3 秒內完成，並允許後續 edit original / follow-up
