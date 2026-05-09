# Tasks: Fiat Order Buyer Notifications

- Date: 2026-04-09
- Feature: Fiat Order Buyer Notifications

## **Task 1: 建單買家通知文案調整**

對應 `R1.1`-`R1.3`，核心目標是在不破壞既有 `/shop` interaction fallback 的前提下，讓建單成功私訊帶出付款期限與逾期取消提醒。

- 1. [x] 更新法幣建單私訊文案
  - 1.1 [x] 在 `FiatOrderService.FiatOrderResult.formatDirectMessage()` 補上付款期限內完成付款與逾期取消提醒。
  - 1.2 [x] 確認 `ShopSelectMenuHandler` 的 interaction 摘要與 DM failure fallback 仍維持既有可付款資訊。

## **Task 2: 付款成功買家私訊通知**

對應 `R2.1`-`R2.3`，核心目標是在 paid callback 後新增買家通知副作用，同時維持既有 idempotency 與履約／管理員通知語意。

- 2. [x] 新增付款成功買家私訊服務並串入 callback
  - 2.1 [x] 建立買家付款成功通知 service，封裝 Discord user lookup、DM 與失敗 logging。
  - 2.2 [x] 在 `FiatPaymentCallbackService` 只對首次成功 paid transition 送出買家付款成功通知，且不影響 duplicate callback 行為。

## **Task 3: 測試與回歸驗證**

對應 `R1.x`、`R2.x` 與 edge cases，核心目標是用 mock Discord DM 狀態驗證通知內容、失敗容錯與 duplicate callback 保護。

- 3. [x] 補強單元／回歸測試
  - 3.1 [x] 在 `FiatOrderServiceTest` 驗證建單私訊文案包含訂單編號、付款期限與逾期取消提醒。
  - 3.2 [x] 在 `FiatPaymentCallbackServiceTest` 驗證付款成功通知的成功、DM 失敗容錯與 duplicate callback 不重複通知。
  - 3.3 [x] 執行最小必要測試並回填 checklist / spec / design / contract。

## Notes
- 本 change 預計只觸及 `shop` 模組內的 service／command／test，避免擴大到 schema 或 callback contract。
- Property-based 測試預計標記 `N/A`：本次屬文案與副作用編排調整，沒有新的可生成型商業邏輯不變式。
