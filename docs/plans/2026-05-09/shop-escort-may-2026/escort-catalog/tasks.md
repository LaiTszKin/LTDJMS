# Tasks: 護航目錄資料庫化與價目表管理

- Date: 2026-05-09
- Feature: 護航目錄資料庫化與價目表管理

## **Task 1: 建立 EscortOptionCatalog domain 與 repository**

Purpose: 建立護航目錄的 domain record 和 repository 介面，為資料庫驅動提供基礎。
Requirements: R2.1, R2.2
Scope: `discord.product.domain`, `discord.product.persistence`
Out of scope: Service 層改寫、管理面板 UI

- 1. [ ] **`src/main/java/ltdjms/discord/product/domain/EscortOptionCatalog.java`** — 新增 record `EscortOptionCatalog(Long id, String code, String type, String level, String mapScope, String target, long priceTwd, Instant createdAt, Instant updatedAt)`；包含 factory method `create(code, type, level, mapScope, target, priceTwd)` 和 `withUpdatedDetails(...)`；驗證必填欄位和 priceTwd > 0
  - Verify: `make build` 編譯通過；record 建構子驗證可拋出 `IllegalArgumentException`

- 2. [ ] **`src/main/java/ltdjms/discord/product/domain/EscortOptionCatalogRepository.java`** — 新增介面：`List<EscortOptionCatalog> findAll()`, `Optional<EscortOptionCatalog> findByCode(String code)`, `EscortOptionCatalog save(EscortOptionCatalog)`, `EscortOptionCatalog update(EscortOptionCatalog)`, `boolean deleteByCode(String code)`, `boolean existsByCode(String code)`, `long count()`
  - Verify: 介面編譯通過

- 3. [ ] **`src/main/java/ltdjms/discord/product/persistence/JdbcEscortOptionCatalogRepository.java`** — JDBC 實作所有介面方法，查詢 `escort_option_catalog` 表；`save()` 使用 `INSERT ... RETURNING id`；`update()` 使用 `UPDATE ... WHERE code = ?`
  - Verify: `make test` 對應 repository 測試通過

## **Task 2: 改寫 EscortOrderOptionCatalog 為 DB 查詢層**

Purpose: 將硬編碼目錄重構為代理新 repository 的相容層，確保過渡期間所有既有引用點不受影響。
Requirements: R5.1, R5.2
Scope: `EscortOrderOptionCatalog.java`
Out of scope: 移除舊類別（留待所有引用點遷移完成後）

- 1. [ ] **`src/main/java/ltdjms/discord/product/domain/EscortOrderOptionCatalog.java`** — 改寫 `createOptions()` 從 `EscortOptionCatalogRepository` 讀取；保留 `findByCode()`, `allOptions()`, `isSupported()`, `supportedCodes()` 等公開方法簽名不變；若 repository 為 null（向後相容），fallback 至硬編碼
  - Verify: 現有參考 `EscortOrderOptionCatalog` 的測試通過；`make build` 通過

## **Task 3: 改寫 EscortOptionPricingService**

Purpose: 更新價格服務使用新的 DB repository 作為護航選項的基礎資料來源。
Requirements: R2.4, R2.5
Scope: `EscortOptionPricingService.java`
Out of scope: 管理面板 UI

- 1. [ ] **`src/main/java/ltdjms/discord/dispatch/services/EscortOptionPricingService.java`** — 建構子新增 `EscortOptionCatalogRepository` 參數；`listOptionPrices()` 從新 repository 讀取所有選項（取代 `EscortOrderOptionCatalog.allOptions()`）；保留公會覆蓋價格合併邏輯；更新 `updateOptionPrice()` 和 `resetOptionPrice()` 的選項驗證改用新 repository
  - Verify: `make test` 通過（更新相關測試的 mock）

- 2. [ ] **`src/main/java/ltdjms/discord/shared/di/DispatchModule.java`** — 新增 `@Provides @Singleton` 方法提供 `JdbcEscortOptionCatalogRepository` 作為 `EscortOptionCatalogRepository`；更新 `EscortOptionPricingService` 的 provider 注入新 repository
  - Verify: Dagger 編譯通過

- 3. [ ] **`src/main/java/ltdjms/discord/shared/di/AppComponent.java`** — 新增 `EscortOptionCatalogRepository` 和相關 service 的 accessor 方法（如需要）
  - Verify: 編譯通過

## **Task 4: 管理面板護航價目表顯示**

Purpose: 在管理面板中新增護航價目表頁面，以商店式 embed 格式顯示所有護航項目。
Requirements: R3.1, R3.2, R3.3
Scope: `AdminPanelButtonHandler.java`, `AdminPanelViewFactory.java`, `AdminPanelService.java`
Out of scope: CRUD 操作（Task 5）

- 1. [ ] **`src/main/java/ltdjms/discord/panel/commands/AdminPanelViewFactory.java`** — 新增 `buildEscortCatalogListEmbed(List<EscortOptionCatalog>, int page, int totalPages)` 方法：以編號列表格式顯示每項的 code、訂單類型、服務範圍、服務價格；每頁 10 項；新增 `buildEscortCatalogComponents(int page, int totalPages)` 方法包含分頁按鈕和 CRUD 按鈕
  - Verify: 編譯通過；embed 格式符合 spec 範例

- 2. [ ] **`src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java`** — 在 `onButtonInteraction()` 新增 `BUTTON_ESCORT_CATALOG = "admin_panel_escort_catalog"` 路由；新增 `showEscortCatalogList()` 方法取代現有 `showEscortPricingConfig()` 的顯示方式
  - Verify: 管理面板主選單出現「護航價目表管理」按鈕；點選後顯示商店式清單

- 3. [ ] **`src/main/java/ltdjms/discord/panel/services/AdminPanelService.java`** — 新增 `getEscortCatalogPage(guildId, page)` 方法封裝分頁查詢（或直接由 handler 呼叫 repository）
  - Verify: 編譯通過

## **Task 5: 護航項目 CRUD 操作**

Purpose: 在護航價目表管理頁面中支援新增、編輯、刪除護航項目。
Requirements: R4.1, R4.2, R4.3, R4.4, R4.5
Scope: `AdminPanelButtonHandler.java`, `AdminPanelViewFactory.java`, `AdminPanelModalFactory.java`
Out of scope: Service 層 CRUD（已在 Task 1 repository 層完成）

- 1. [ ] **`src/main/java/ltdjms/discord/panel/commands/AdminPanelViewFactory.java`** — 在護航價目表 components 中加入「新增項目」、「編輯」、「刪除」按鈕
  - Verify: 按鈕正確顯示在管理面板中

- 2. [ ] **`src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java`** — 新增 `BUTTON_ESCORT_CATALOG_CREATE` / `BUTTON_ESCORT_CATALOG_EDIT_` / `BUTTON_ESCORT_CATALOG_DELETE_` 按鈕處理：Create 彈出 Modal、Edit 彈出預填 Modal、Delete 執行刪除；新增 `MODAL_ESCORT_CATALOG_CREATE` / `MODAL_ESCORT_CATALOG_EDIT_` modal handler
  - Verify: 新增/編輯/刪除操作成功後刷新列表

- 3. [ ] **`src/main/java/ltdjms/discord/panel/commands/AdminPanelModalFactory.java`** — 新增 `createEscortCatalogCreateModal()` 和 `createEscortCatalogEditModal(EscortOptionCatalog)` 方法；Modal 包含 code、type、level、mapScope、target、priceTwd 六個欄位
  - Verify: Modal 格式正確、欄位驗證正確

## Notes
- Task 1 為基礎設施（可先實作但需 V028 遷移先完成）
- Task 2-3 為服務層重構（相依於 Task 1）
- Task 4-5 為管理面板 UI（相依於 Task 2-3 的 repository/service）
- `preparation.md` 的 V028 遷移是 Task 1 的前置條件
- 現有硬編碼目錄在 Task 2 改寫後保留 fallback 能力
