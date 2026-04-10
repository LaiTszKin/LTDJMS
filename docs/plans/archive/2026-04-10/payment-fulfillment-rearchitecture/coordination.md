# Coordination: payment-fulfillment-rearchitecture

- Date: 2026-04-10
- Batch: payment-fulfillment-rearchitecture
- Goal: 將法幣付款成功後的副作用從 callback 主路徑拆出為可重試的背景處理，並移除不再需要的外部履約設定與商品面板欄位。

## Shared Direction
- 付款真相來源維持為 `fiat_order`；ECPay callback 或補償查單一旦確認成功，即只負責把訂單標記為 `PAID`。
- 發貨、護航開單、管理員通知改由背景 worker 從資料庫待處理狀態拉取並冪等執行，不再在 callback request thread 內直接完成。
- 舊的外部 backend fulfillment webhook 視為淘汰路徑；本批次完成後不再需要商品 `backendApiUrl`、`PRODUCT_FULFILLMENT_SIGNING_SECRET` 與 `ECPAY_CALLBACK_SHARED_SECRET`。

## Spec Ownership
- `fiat-payment-reconciliation-worker/`
  - owns：`shop/` 付款 callback、補償查單、背景 worker、`fiat_order` schema 與相關測試
  - may touch：`src/main/java/ltdjms/discord/shop/**`、`src/main/resources/db/**`、必要的 Dagger wiring
- `remove-legacy-backend-fulfillment-config/`
  - owns：`product/`、admin panel、`EnvironmentConfig`、文件中的淘汰設定與欄位清理
  - may touch：`src/main/java/ltdjms/discord/product/**`、`src/main/java/ltdjms/discord/panel/**`、`src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`、`docs/**`

## Shared Constraints
- 不可改變既有 `MerchantTradeNo` / `orderNumber` 語意。
- 不可讓 callback 成功與否決定發貨是否永久遺失；任何副作用都必須可重試。
- 任何資料庫 claim / retry 欄位都必須保持多次 callback、worker 重啟、重複排程下的冪等。
- 若移除商品 `backendApiUrl`，同批次必須同步移除所有程式與 UI 上對它的必填假設。

## Merge / Rollout Order
- 建議先完成 `fiat-payment-reconciliation-worker`，再完成 `remove-legacy-backend-fulfillment-config`。
- 實作時可同一輪落地，但若拆提交，必須先讓內部護航開單與發貨 worker 可在沒有 `backendApiUrl` 的前提下工作，再移除欄位與設定。

## Post-Merge Verification
- 驗證 ECPay paid callback 只負責落庫與喚醒處理，不再直接打外部履約。
- 驗證背景 worker 可從 `PAID` 未完成訂單補做管理員通知 / 護航開單 / fulfilled 標記。
- 驗證商品建立、編輯、管理面板與文件已不再暴露 `backendApiUrl` 與相關 secret。
