# Contract: 護航購買通知

- Date: 2026-04-28
- Feature: 護航購買通知
- Change Name: escort-purchase-notifications

## Purpose
此變更主要仰賴 JDA（Java Discord API）的非同步 DM 發送機制，以及內部抽象層 `DiscordRuntimeGateway`。外部依賴有限，聚焦於正確使用既有的 Discord DM 模式。

## Usage Rule
- 每個外部依賴若對實作產生實質約束（API shape、限制、錯誤模式），則記錄一個依賴紀錄。
- 若無外部依賴實質影響此變更，則在 `## Dependency Records` 下寫 `None` 並簡短解釋。
- 所有主張須有官方文件或實作中已驗證的來源支援。

## Dependency Records

### Dependency 1: JDA (Java Discord API) — DM 發送

- Type: library
- Version / Scope: 5.2.2（由 `pom.xml` 管理），用於 `User.openPrivateChannel()` / `PrivateChannel.sendMessage()`
- Official Source: https://github.com/discord-jda/JDA（專案 pom.xml）
- Why It Matters: 這是發送 Discord DM 的唯一方式，所有通知最終都依賴 JDA 的非同步佇列機制
- Invocation Surface:
  - Entry points: `User.openPrivateChannel()`, `PrivateChannel.sendMessage(CharSequence)`, `RestAction.queue(Consumer, Consumer)`
  - Call pattern: 非同步 callback（`queue(success, failure)`），受 Discord API rate limit 約束
  - Required inputs: 有效的 `User` 物件（需先由 `retrieveUserById` 解析）、DM channel 必須存在且未被用戶關閉
  - Expected outputs: `MessageCreateAction` 佇列成功後，用戶收到 DM 訊息；失敗時由 failure callback 處理
- Constraints:
  - Supported behavior: 向任何有效使用者發送文字 DM（純文字字串）
  - Limits: Discord API rate limit（每個 guild / 每個使用者）；單一訊息最大 2000 字元
  - Compatibility: 僅限 Discord 平台；需要 bot 與使用者共用同一個 guild
  - Security / access: bot 需有隱含權限，無法對已封鎖 bot 或關閉 DM 的使用者發送
- Failure Contract:
  - Error modes:
    - 使用者已封鎖 bot → failure callback 收到 `ErrorResponse.CANNOT_MESSAGE_USER`
    - 使用者關閉了 DM → `openPrivateChannel()` failure callback
    - 使用者不存在或離開 guild → `retrieveUserById` failure callback
    - Discord API 逾時或暫時性錯誤 → HTTP 5xx / timeout
  - Caller obligations: 必須提供 success 與 failure callback；不得 blocking wait；必須處理 failure callback（至少 log）
  - Forbidden assumptions: 不應假設 DM 一定成功；不應阻塞等待 `queue()` 完成
- Verification Plan:
  - Spec mapping: R1, R2, R3, R4
  - Design mapping: `EscortOrderBuyerNotificationService` / `ShopAdminNotificationService` 中的 `sendAdminNotification` 方法
  - Planned coverage: UT-01（mock retrieveUserById）、UT-02（驗證訊息字串）、UT-05（驗證管理員訊息格式）
  - Evidence notes: 現有 `ShopAdminNotificationService.sendAdminNotification()` 已使用完全相同模式

### Dependency 2: DiscordRuntimeGateway（內部抽象層）

- Type: 內部抽象層（非外部依賴，但統整 Discord 執行時期存取）
- Version / Scope: 專案內部介面，實作為 `JdaDiscordRuntimeGateway`
- Official Source: `shared/runtime/DiscordRuntimeGateway.java`
- Why It Matters: 所有 service 層透過此 gateway 取得 `User` 和 `Guild`，而非直接依賴 JDA；需使用此抽象以維持可測試性
- Invocation Surface:
  - Entry points: `retrieveUserById(long)`, `getGuildById(long)`, `getSelfUserId()`
  - Call pattern: `retrieveUserById` 回傳 `RestAction<User>`（非同步），`getGuildById` 和 `getSelfUserId` 為同步
  - Required inputs: Discord 使用者 ID（long）、Guild ID（long）
  - Expected outputs: `RestAction<User>` 可 chain 呼叫 `queue()`；`Guild` 物件或 null（若 bot 不在該 guild）
- Constraints:
  - Supported behavior: 標準 JDA `RestAction` 回傳
  - Limits: 無特殊限制，受 JDA 本身限制定義
  - Compatibility: 所有 service 均使用此 gateway
  - Security / access: 無特殊權限檢查
- Failure Contract:
  - Error modes: `retrieveUserById` 若使用者不存在則 failure callback
  - Caller obligations: 必須提供 failure callback
  - Forbidden assumptions: 不應假設 `retrieveUserById` 一定成功
- Verification Plan:
  - Spec mapping: R3, R4
  - Design mapping: `EscortOrderBuyerNotificationService`
  - Planned coverage: UT-01, UT-03, UT-04（mock gateway）
  - Evidence notes: 已有 `ShopAdminNotificationServiceTest` 展示 mock 模式
