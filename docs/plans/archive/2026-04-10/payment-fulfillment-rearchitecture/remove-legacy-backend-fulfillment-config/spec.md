# Spec: Remove legacy backend fulfillment config

- Date: 2026-04-10
- Feature: Remove legacy backend fulfillment config
- Owner: Codex

## Goal
移除商品與部署中不再需要的外部履約設定，讓自動護航開單與付款後處理完全回歸 bot 內部實作。

## Scope

### In Scope
- 移除商品 `backendApiUrl` 的建立、編輯、驗證、顯示與持久化依賴
- 移除 `PRODUCT_FULFILLMENT_SIGNING_SECRET` 與 `ECPAY_CALLBACK_SHARED_SECRET` 的 config surface、文件與樣板
- 讓 `autoCreateEscortOrder` 只依賴內部護航選項，不再要求 backend URL
- 清理文件、管理面板與 migration/schema 中的舊說明或欄位

### Out of Scope
- 移除 `autoCreateEscortOrder` 或 `escortOptionCode` 功能本身
- 大幅重做商品面板的其他互動

## Functional Behaviors (BDD)

### Requirement 1: 商品配置不得再要求 backend fulfillment URL
**GIVEN** 管理員建立或編輯商品  
**AND** 商品可啟用自動護航開單  
**WHEN** 管理員提交商品設定  
**THEN** 系統不得再要求輸入 `backendApiUrl`  
**AND** 啟用自動護航開單時只需驗證內部 `escortOptionCode`

**Requirements**:
- [x] R1.1 `Product` / `ProductService` 不再以 `backendApiUrl` 作為有效性條件
- [x] R1.2 admin product panel 不再顯示或收集 backend URL 欄位
- [x] R1.3 現有商品資料若有遺留 backend URL，不得影響功能

### Requirement 2: 舊 env/config 不得再暴露為可設定面
**GIVEN** operator 使用 `.env.example`、`application.properties` 或文件設定系統  
**WHEN** 檢視支付與履約相關設定  
**THEN** 不應再看到 `PRODUCT_FULFILLMENT_SIGNING_SECRET` 或 `ECPAY_CALLBACK_SHARED_SECRET` 被描述為有效設定  
**AND** 系統程式碼也不得再讀取它們作為執行條件

**Requirements**:
- [x] R2.1 `EnvironmentConfig` 不再映射與暴露這兩個設定
- [x] R2.2 文件與樣板必須移除或明確淘汰這些設定
- [x] R2.3 runtime 不得因未設定這些值而改變付款/發貨結果

### Requirement 3: 舊 backend fulfillment persistence 應被安全清理
**GIVEN** 系統已不再依賴商品 backend URL  
**WHEN** migration 與 repository schema 被更新  
**THEN** 新資料模型不得再把 backend URL 作為核心欄位  
**AND** 清理流程不得破壞既有商品、法幣訂單與護航選項資料

**Requirements**:
- [x] R3.1 schema / repository mapping 必須與新的 Product 模型一致
- [x] R3.2 若執行資料遷移刪欄，必須先保證 runtime 已不再讀寫舊欄位
- [x] R3.3 測試需覆蓋舊資料存在下的讀取與更新兼容性

## Error and Edge Cases
- [x] 舊商品資料有 `backend_api_url` 值時，更新商品不得失敗
- [x] auto escort 啟用但 `escortOptionCode` 缺失時仍需正確拒絕
- [x] 文件、設定範本與 runtime config 不能出現不一致
- [x] migration 不得讓既有 product row 因 constraint 失效而無法升級
- [x] UI 清理後，不得殘留引用 backend URL 的 summary 或 modal

## Clarification Questions
None

## References
- Official docs:
  - None（本 spec 主要是本地架構與設定清理）
- Related code files:
  - `src/main/java/ltdjms/discord/product/domain/Product.java`
  - `src/main/java/ltdjms/discord/product/services/ProductService.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminProductPanelHandler.java`
  - `src/main/java/ltdjms/discord/shared/EnvironmentConfig.java`
