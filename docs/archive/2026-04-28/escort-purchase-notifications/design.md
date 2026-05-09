# Design: 護航購買通知

- Date: 2026-04-28
- Feature: 護航購買通知
- Change Name: escort-purchase-notifications

## Design Goal
實作護航商品購買後的兩組 DM 通知：管理員接到有新訂單需派單，買家接到訂單等待處理。

## Change Summary
- Requested change: 護航商品購買後 DM 通知管理人員（含客戶、訂單、付款狀態）以及 DM 通知買家訂單等待處理
- Existing baseline:
  - 管理員通知已有 `ShopAdminNotificationService`，但未明確標示「付款狀態」
  - 買家通知在法幣流程已有 `FiatOrderBuyerNotificationService`，但內容不含護航相關資訊
  - 貨幣購買流程完全沒有買家通知
- Proposed design delta:
  - 新增 `EscortOrderBuyerNotificationService` 負責 DM 買家護航訂單等待處理
  - 在 `ShopSelectMenuHandler` 貨幣購買流程中呼叫新 service
  - 在 `FiatOrderPostPaymentWorker` 法幣付款流程中呼叫新 service
  - 擴充 `ShopAdminNotificationService` 訊息含明確付款狀態

## Scope Mapping
- Spec requirements covered: R1, R2, R3, R4
- Affected modules:
  - `shop/services/` — 新增 `EscortOrderBuyerNotificationService`，修改 `ShopAdminNotificationService`
  - `shop/commands/` — 修改 `ShopSelectMenuHandler`
  - `shop/services/` — 修改 `FiatOrderPostPaymentWorker`
- External contracts involved: JDA DM channel API（見 contract.md）
- Coordination reference: None（單一 spec，不需跨 spec 協調）
- Parallel implementation assumption: None（單一 spec）

## Current Architecture

```
┌─ 貨幣購買 ───────────────────────────────────┐
│ ShopSelectMenuHandler.onButtonInteraction()   │
│   → CurrencyPurchaseService.purchaseProduct() │
│   → EscortDispatchHandoffService.handoff()    │
│   → ShopAdminNotificationService              │  ✅ 管理員通知（但無付款狀態）
│   → reply ephemeral success                   │  ❌ 無買家通知
└───────────────────────────────────────────────┘

┌─ 法幣付款 ──────────────────────────────────────┐
│ FiatOrderPostPaymentWorker.processSingleOrder() │
│   → FiatOrderBuyerNotificationService            │  ✅ 付款成功通知（但無護航資訊）
│   → EscortDispatchHandoffService.handoff()       │
│   → ShopAdminNotificationService                 │  ✅ 管理員通知（但無付款狀態）
│   → ProductRewardService.grantReward()           │
│   → markFulfilled                               │
└──────────────────────────────────────────────────┘
```

## Proposed Architecture

```
┌─ 貨幣購買 ──────────────────────────────────────────┐
│ ShopSelectMenuHandler.onButtonInteraction()          │
│   → CurrencyPurchaseService.purchaseProduct()        │
│   → EscortDispatchHandoffService.handoff()           │
│   → [NEW] EscortOrderBuyerNotificationService  🆕   │  ✅ 買家護航通知
│   → ShopAdminNotificationService (enhanced)          │  ✅ 付款狀態
│   → reply ephemeral success                          │
└──────────────────────────────────────────────────────┘

┌─ 法幣付款 ─────────────────────────────────────────────┐
│ FiatOrderPostPaymentWorker.processSingleOrder()        │
│   → FiatOrderBuyerNotificationService                   │  ✅（不變）
│   → EscortDispatchHandoffService.handoff()              │
│   → [NEW] EscortOrderBuyerNotificationService  🆕      │  ✅ 買家護航通知
│   → ShopAdminNotificationService (enhanced)             │  ✅ 付款狀態
│   → ProductRewardService.grantReward()                  │
│   → markFulfilled                                      │
└─────────────────────────────────────────────────────────┘
```

## Component Changes

### Component 1: `EscortOrderBuyerNotificationService`（新增）

- Responsibility: 當護航交接單自動建立後，DM 通知買家訂單正在等待處理
- Inputs:
  - `EscortDispatchOrder` 實體（含 `customerUserId`、`sourceProductName`、`orderNumber`、`sourceCurrencyPrice`、`sourceFiatPriceTwd`、`sourceType`）
  - `DiscordRuntimeGateway`（注入）
- Outputs: 發送 Discord DM 給 `customerUserId`
- Dependencies: `DiscordRuntimeGateway`
- Invariants:
  - 呼叫前需確保 `EscortDispatchOrder` 非 null
  - 發送失敗僅 log warn，不拋例外、不影響呼叫端流程

**訊息範例（貨幣購買）：**
```
🛡️ 護航訂單已建立，正在等待處理

商品：VIP 護航方案
護航訂單編號：`ESC-20260428-ABC123`
付款方式：貨幣（100,000 貨幣）

我們已收到你的訂單，管理員將會在不久後為你安排護航，請耐心等候。
```

**訊息範例（法幣付款）：**
```
🛡️ 護航訂單已建立，正在等待處理

商品：VIP 護航方案
護航訂單編號：`ESC-20260428-ABC123`
付款方式：法幣（NT$1,200）

我們已收到你的訂單，管理員將會在不久後為你安排護航，請耐心等候。
```

### Component 2: `ShopSelectMenuHandler`（修改）

- 新增依賴：`EscortOrderBuyerNotificationService`
- 修改點：`onButtonInteraction()` 方法中，在貨幣購買成功且 handoff 成功後，呼叫 `escortOrderBuyerNotificationService.notifyEscortOrderCreated(handoffResult.getValue())`
- 不改變：現有的 ephemeral reply、admin notification、handoff 邏輯

### Component 3: `FiatOrderPostPaymentWorker`（修改）

- 新增依賴：`EscortOrderBuyerNotificationService`
- 修改點：`processSingleOrder()` 方法中，在 escort handoff 成功後且在 `adminNotificationService.notifyAdminsOrderCreated()` 完成後，呼叫 `escortOrderBuyerNotificationService.notifyEscortOrderCreated(dispatchOrder)`

### Component 4: `ShopAdminNotificationService`（增強）

- 修改點：`buildAdminOrderNotification(Guild, long, EscortDispatchOrder)` 方法
- 增加一行顯式付款狀態，格式：
  - `CURRENCY_PURCHASE` → `**付款狀態：** 已付款（貨幣）`（若有價格則附加金額）
  - `FIAT_PAYMENT` → `**付款狀態：** 已付款（法幣）`（若有價格則附加金額）

**增強後通知範例：**
```
📩 有新護航工作交接，請儘速處理

伺服器：測試伺服器 (123456789)
買家：<@987654321>
來源類型：貨幣購買
來源參考：ORDER-001
來源商品：VIP 護航方案
付款狀態：已付款（貨幣 100,000）
護航選項：`escort-a`
Dispatch 編號：`ESC-20260428-ABC123`

請使用 `/dispatch-panel` 檢視或後續處理此工作項。
```

## Sequence / Control Flow

### 貨幣購買流程：
1. 用戶點擊確認購買按鈕
2. `CurrencyPurchaseService.purchaseProduct()` 執行扣款與發獎
3. 若商品有護航服務且 handoff 成功：
   a. `EscortOrderBuyerNotificationService.notifyEscortOrderCreated()` → DM 買家
   b. `ShopAdminNotificationService.notifyAdminsOrderCreated()` → DM 管理員（含付款狀態）
4. ephemeral reply 回傳成功訊息

### 法幣付款流程：
1. `FiatOrderPostPaymentWorker.processSingleOrder()` 處理已付款訂單
2. `FiatOrderBuyerNotificationService.notifyPaymentSucceeded()` → DM 買家付款成功（現有）
3. `EscortDispatchHandoffService.handoffFromFiatPayment()` → 建立護航交接單
4. 若 handoff 成功：
   a. `EscortOrderBuyerNotificationService.notifyEscortOrderCreated()` → DM 買家護航通知
   b. `ShopAdminNotificationService.notifyAdminsOrderCreated()` → DM 管理員（含付款狀態）

## Data / State Impact
- Created or updated data: 無（不新增欄位、不新增資料表）
- Consistency rules: 不影響既有資料一致性
- Migration / rollout needs: None

## Risk and Tradeoffs
- Key risks:
  - DM 發送失敗：兩組通知均採 best-effort，log warn 不阻斷流程
  - 法幣流程中買家先收到付款成功通知，再收到護航等待通知（非同步）— 此順序可被接受
  - Dagger DI 配置遺漏：需確保新 service 註冊到正確 module
- Rejected alternatives:
  - 擴充 `FiatOrderBuyerNotificationService` 同時處理護航通知：名稱與職責不符（fiat-specific），且貨幣購買流程無法共用
  - 合併買家付款成功通知與護航通知：法幣流程中此兩通知時機不同（付款確認時 vs handoff 後），分開較清晰
  - 為買家通知加入持久化追蹤：現有法幣 `buyerNotifiedAt` 僅適用於付款成功通知，新增欄位成本高於價值
- Operational constraints: 無特殊營運考量
- Cross-spec collision notes: None

## Validation Plan
- Tests: 單元測試涵蓋新 service 以及各觸發點的呼叫（UT-01 至 UT-06）
- Contract checks: 透過 mock `DiscordRuntimeGateway` 驗證 JDA 呼叫模式
- Rollback / fallback: 此變更僅新增通知，不影響既有購買與付款流程；可安全 rollback

## Open Questions

None — 設計已底定，可進入實作。
