# Design: Remove legacy backend fulfillment config

- Date: 2026-04-10
- Feature: Remove legacy backend fulfillment config
- Change Name: remove-legacy-backend-fulfillment-config

## Design Goal
刪除舊的外部履約設定面，使商品配置與部署設定只保留仍有執行價值的欄位。

## Change Summary
- Requested change: 移除不再需要的 backend fulfillment 參數與商品欄位
- Existing baseline: `Product`、`ProductService`、admin panel 與 `EnvironmentConfig` 仍保留 `backendApiUrl`、`PRODUCT_FULFILLMENT_SIGNING_SECRET`、`ECPAY_CALLBACK_SHARED_SECRET`
- Proposed design delta: 保留內部護航配置，移除外部 backend fulfillment configuration 與對應 schema/文件

## Scope Mapping
- Spec requirements covered: `R1.1-R3.3`
- Affected modules: `product`, `panel`, `shared config`, `db migration`, `docs`
- External contracts involved: `None`
- Coordination reference: `../coordination.md`

## Current Architecture
- 商品資料模型包含 `backendApiUrl`、`autoCreateEscortOrder`、`escortOptionCode`
- 目前 `autoCreateEscortOrder=true` 時，`Product` 與 `ProductService` 都要求 `backendApiUrl`
- admin panel 會顯示並要求輸入 backend URL
- runtime config 仍保留 webhook signing secret 與 callback shared secret

## Proposed Architecture
- 商品模型不再保留 `backendApiUrl` 作為業務必要欄位
- `autoCreateEscortOrder` 改為純內部功能，只依賴 `escortOptionCode`
- admin panel 移除 backend URL UI
- `EnvironmentConfig`、properties 與 docs 清除 obsolete settings
- migration 在 runtime 不再依賴舊欄位後移除 `product.backend_api_url` 與其 constraint/index/comment

## Component Changes

### Component 1: `Product` / `ProductService`
- Responsibility: 驗證並持久化新的商品設定語意
- Inputs: 商品建立/編輯參數
- Outputs: 不含 backend URL 依賴的 `Product`
- Dependencies: `ProductRepository`
- Invariants: `autoCreateEscortOrder` 啟用時必須有有效 `escortOptionCode`

### Component 2: `AdminProductPanel*`
- Responsibility: 顯示與收集仍受支援的商品欄位
- Inputs: panel session state / modal values
- Outputs: 商品編輯請求
- Dependencies: `ProductService`
- Invariants: 不再出現 backend URL 欄位或文案

### Component 3: `EnvironmentConfig` / migration / docs
- Responsibility: 清理 obsolete config 與欄位
- Inputs: `.env`, application config, DB schema
- Outputs: 簡化後的設定面與 schema
- Dependencies: runtime startup, Flyway
- Invariants: 升級後既有商品與法幣訂單資料仍可正確讀取

## Sequence / Control Flow
1. runtime 先改為不再讀寫 backend URL / obsolete secrets
2. admin panel 與 product service 移除 backend URL 相關互動與驗證
3. schema migration 清理舊欄位與 constraint
4. docs / config templates 同步移除舊設定說明

## Data / State Impact
- Created or updated data: `product` table 移除 `backend_api_url` 與相關 constraint/comment
- Consistency rules: product model、repository mapping、panel state、config docs 必須一致
- Migration / rollout needs: 必須先讓 runtime 不再引用舊欄位，再執行 drop column

## Risk and Tradeoffs
- Key risks: 舊 repository mapping 與 migration 順序不一致、panel 殘留 backend URL 欄位、文件與 runtime 漂移
- Rejected alternatives:
  - 只在文件隱藏設定而不刪程式：會留下死配置與後續混亂
  - 先 drop column 再改 runtime：升級期間會直接破壞現有程式
- Operational constraints: migration 必須可在既有資料上平滑執行

## Validation Plan
- Tests: `UT-product-no-backend-url`, `UT-panel-no-backend-url`, `IT-product-migration-cleanup`
- Contract checks: `None`
- Rollback / fallback: 若 migration 風險過高，可先做 runtime cleanup，drop column 以單獨 migration 後移

## Open Questions
- 已解答：依使用者批准，本批次直接 drop `backend_api_url`
