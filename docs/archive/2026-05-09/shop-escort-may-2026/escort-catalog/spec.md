# Spec: 護航目錄資料庫化與價目表管理

- Date: 2026-05-09
- Feature: 護航目錄資料庫化與價目表管理
- Owner: [To be filled]

## Goal

將護航價目從硬編碼改為資料庫驅動，並在管理面板中提供商店式價目表顯示和完整 CRUD 管理功能。

## Scope

### In Scope
- 建立 `escort_option_catalog` 資料表取代硬編碼目錄
- 遷移現有 26 個護航選項為種子資料
- 新增 `EscortOptionCatalogRepository` 介面和 JDBC 實作
- 改寫 `EscortOptionPricingService` 使用新 repository
- 管理面板中新增「護航價目表管理」頁面，以類似商店的 embed 格式顯示
- 價目表支援完整 CRUD：新增、修改、刪除護航項目及其類型、範圍、價格
- 保留公會層級價格覆蓋（`guild_escort_option_price` 表）

### Out of Scope
- 護航派單流程變更（`EscortDispatchHandoffService` 不變）
- 商品與護航選項的關聯設定（`auto_create_escort_order` 欄位不變）
- 商店頁面本身的變更
- 綠界付款流程

## Functional Behaviors (BDD)

### Requirement 1: 護航目錄持久化
**GIVEN** 系統啟動
**WHEN** Flyway 遷移 V028 執行
**THEN** `escort_option_catalog` 表被建立並包含 26 筆種子資料
**AND** 每筆資料包含 code、type、level、map_scope、target、price_twd 六個欄位

**Requirements**:
- [ ] R1.1 建立 `escort_option_catalog` 表，欄位：`id BIGSERIAL PK`, `code VARCHAR(120) UNIQUE NOT NULL`, `type VARCHAR(64) NOT NULL`, `level VARCHAR(64) NOT NULL`, `map_scope VARCHAR(256) NOT NULL`, `target VARCHAR(256) NOT NULL`, `price_twd BIGINT NOT NULL CHECK (price_twd > 0)`, `created_at`, `updated_at`
- [ ] R1.2 匯入 26 筆種子資料（從現有 `EscortOrderOptionCatalog` 複製）
- [ ] R1.3 表支援完整 CRUD 操作

### Requirement 2: Repository 與 Service 層重構
**GIVEN** `escort_option_catalog` 表存在
**WHEN** 查詢所有護航選項
**THEN** 從資料庫讀取而非從硬編碼 `EscortOrderOptionCatalog`

**GIVEN** 公會有自訂價格覆蓋
**WHEN** 查詢該公會的護航價目表
**THEN** 顯示有效價格（DB 預設價格 + 公會覆蓋）

**Requirements**:
- [ ] R2.1 新增 `EscortOptionCatalog` domain record（對應 `escort_option_catalog` 表）
- [ ] R2.2 新增 `EscortOptionCatalogRepository` 介面：`findAll()`, `findByCode(String)`, `save()`, `update()`, `deleteByCode(String)`, `existsByCode(String)`
- [ ] R2.3 新增 `JdbcEscortOptionCatalogRepository` JDBC 實作
- [ ] R2.4 改寫 `EscortOptionPricingService.listOptionPrices()` 使用新 repository 而非 `EscortOrderOptionCatalog`
- [ ] R2.5 `EscortOptionPricingService.updateOptionPrice()` 驗證 option code 時查詢 DB 而非硬編碼目錄

### Requirement 3: 管理面板價目表顯示（商店式）
**GIVEN** 管理員開啟管理面板並進入「護航價目表管理」
**WHEN** 頁面載入
**THEN** 以類似商店的 embed 格式顯示所有護航項目，每項包含編號、名稱（code）、訂單類型、服務範圍、服務價格
**AND** 格式如：`1. CONF_DAM_300W: 訂單類型：包本單 / 服務範圍：機密護 / 服務價格：NT$500`
**AND** 支援分頁（每頁 10 項）

**Requirements**:
- [ ] R3.1 管理面板主選單新增「護航價目表管理」按鈕
- [ ] R3.2 價目表 embed 以編號列表格式顯示，每項顯示：訂單類型、服務範圍、服務價格
- [ ] R3.3 支援分頁瀏覽

### Requirement 4: 護航項目 CRUD
**GIVEN** 管理員在護航價目表管理頁面
**WHEN** 點選「新增項目」按鈕
**THEN** 彈出 Modal 輸入 code、type、level、map_scope、target、price_twd
**AND** 提交後新增至資料庫並刷新列表

**GIVEN** 管理員選擇一個既有項目
**WHEN** 點選「編輯」按鈕
**THEN** 彈出 Modal 預填現有值，修改後更新資料庫

**GIVEN** 管理員選擇一個既有項目
**WHEN** 點選「刪除」按鈕
**THEN** 確認後從資料庫刪除

**Requirements**:
- [ ] R4.1 支援新增護航項目（Modal → repository.save()）
- [ ] R4.2 支援編輯護航項目（Modal 預填 → repository.update()）
- [ ] R4.3 支援刪除護航項目（確認 → repository.deleteByCode()）
- [ ] R4.4 新增/編輯時驗證必填欄位和價格 > 0
- [ ] R4.5 code 唯一性驗證

### Requirement 5: 向後相容
**GIVEN** 護航目錄已資料庫化
**WHEN** 現有功能使用護航選項（`EscortDispatchOrderService`、商品接入設定）
**THEN** 選項查詢和驗證仍正常運作

**Requirements**:
- [ ] R5.1 `EscortOrderOptionCatalog` 改寫為查詢層，代理至新 repository（保留公開 API 直到所有引用點遷移）
- [ ] R5.2 現有護航派單、商品接入設定不受影響

## Error and Edge Cases
- [ ] 刪除被商品引用的護航選項時的處理（soft constraint：提示管理員該選項正被 X 個商品引用）
- [ ] code 重複檢查（新增/編輯時）
- [ ] 非管理員無法存取護航價目表管理頁面
- [ ] 資料庫異常時的錯誤處理（顯示錯誤訊息）
- [ ] 分頁邊界：無項目或最後一頁

## Clarification Questions
None — 需求已在對話中確認。

## References
- Official docs:
  - PostgreSQL: https://www.postgresql.org/docs/current/
  - Flyway: https://flywaydb.org/documentation/
- Related code files:
  - `src/main/java/ltdjms/discord/product/domain/EscortOrderOptionCatalog.java` — 現有硬編碼目錄
  - `src/main/java/ltdjms/discord/dispatch/services/EscortOptionPricingService.java` — 價格服務
  - `src/main/java/ltdjms/discord/dispatch/domain/EscortOptionPriceRepository.java` — 公會覆蓋價 repository
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java` — 管理面板按鈕處理
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelViewFactory.java` — 管理面板 view factory
  - `src/main/java/ltdjms/discord/panel/services/AdminPanelService.java` — 管理面板服務
  - `src/main/java/ltdjms/discord/shared/di/DispatchModule.java` — Dagger DI
  - `src/main/resources/db/migration/V019__create_guild_escort_option_price.sql`
