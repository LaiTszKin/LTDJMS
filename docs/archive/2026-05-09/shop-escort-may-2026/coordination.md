# Coordination: 商店與護航定價優化

- Date: 2026-05-09
- Batch: shop-escort-may-2026

## Business Goals

將商店體驗現代化（按名稱排序、合併購買按鈕、關鍵字搜尋）並將護航定價從硬編碼改為資料庫驅動，支援管理員在管理面板內以商店式介面完整 CRUD 護航項目與價目。

- Batch members: [shop-escort, escort-catalog]
- Shared outcome: 一般成員在商店中按名稱排序瀏覽商品、用關鍵字搜尋、統一的購買流程；管理員可在管理面板中以分頁清單管理護航項目與價目。
- Parallel readiness: Yes — 兩組規格觸及不同的服務層，唯一共享的 `discord.product` 模組以 additive-only 方式新增介面方法，可獨立實作後合併。
- Out of scope: 商品管理面板本身的重構、貨幣經濟設定變更、護航派單流程變更。

## Design Principles

- Current baseline: 商店依建立時間倒序排列（`ORDER BY created_at DESC`），貨幣購買（`shop_purchase`）與法幣下單（`shop_fiat_order`）各有一個按鈕；護航價目由 `EscortOrderOptionCatalog` 硬編碼提供 26 個選項，公會覆蓋價格存於 `guild_escort_option_price` 表。
- Shared invariants: Product domain record 不變；`Result<T, DomainError>` monad 模式保持；所有服務層透過 Dagger DI 注入；Flyway 遷移遵循 idempotent 慣例。
- Shared constraints: JDBC 直接操作資料庫（無 ORM）；Discord embed field value 上限 1024 字元；每頁 5 項（商店）/ 10 項（管理面板）。
- Legacy direction: `EscortOrderOptionCatalog` 硬編碼目錄將被新 DB 表取代，遷移後移除舊類別。
- Compatibility window: `EscortOrderOptionCatalog` 暫時保留為查詢層相容層，escort-catalog spec 完成後移除。
- Cleanup after cutover: 刪除 `EscortOrderOptionCatalog.java`，更新所有引用點指向新 repository。

## Spec Boundaries

### Ownership Map

#### Spec Set 1: shop-escort
- Primary concern: 商店頁面體驗優化（排序、合併購買按鈕、關鍵字搜尋）
- Allowed touch points:
  - `src/main/java/ltdjms/discord/shop/**` — 所有商店相關檔案
  - `src/main/java/ltdjms/discord/product/domain/ProductRepository.java` — 新增 `findByGuildIdAndNameContaining()` 方法簽名
  - `src/main/java/ltdjms/discord/product/persistence/JdbcProductRepository.java` — 修改排序、新增搜尋 SQL
  - `src/main/java/ltdjms/discord/product/services/ProductService.java` — 新增搜尋服務方法
- Must not change: `discord.dispatch/**`、`discord.panel/**`、`EscortOrderOptionCatalog.java`

#### Spec Set 2: escort-catalog
- Primary concern: 護航目錄資料庫化、商店式價目表顯示、管理面板完整 CRUD
- Allowed touch points:
  - `src/main/resources/db/migration/V028__*.sql` — 新增遷移
  - `src/main/java/ltdjms/discord/product/domain/EscortOrderOptionCatalog.java` — 重構為 DB 查詢層
  - `src/main/java/ltdjms/discord/product/domain/` — 新增 `EscortOptionCatalog` domain、`EscortOptionCatalogRepository` 介面
  - `src/main/java/ltdjms/discord/product/persistence/` — 新增 `JdbcEscortOptionCatalogRepository`
  - `src/main/java/ltdjms/discord/dispatch/services/EscortOptionPricingService.java` — 改用新 repository
  - `src/main/java/ltdjms/discord/dispatch/domain/EscortOptionPriceRepository.java` — 保留（公會覆蓋價）
  - `src/main/java/ltdjms/discord/panel/` — 新增 `AdminEscortCatalogHandler` + 對應 view
  - `src/main/java/ltdjms/discord/shared/di/` — 註冊新 repository 與 service
- Must not change: `discord/shop/**`、現有 ProductRepository 方法（可擴充不可移除）

### Collisions & Integration

- Shared files & edit rules:
  - `ProductRepository.java` — 兩個 spec 都可能新增方法。Additive-only：各自在介面尾端新增，合併無衝突。
  - `EscortOrderOptionCatalog.java` — escort-catalog 擁有修改權；shop-escort 不得觸碰。
  - Dagger `AppComponent.java` / module files — 各自在對應模組新增 `@Provides`，無位置衝突。
- Shared API / schema freeze:
  - V028 migration（護航目錄表）由 escort-catalog 擁有。
  - `EscortOrderOptionCatalog` 公開 API 在過渡期間保持向後相容。
- Compatibility shim retention: `EscortOrderOptionCatalog` 暫時保留直到 escort-catalog 確認所有呼叫點已遷移。
- Merge order: 獨立合併（建議先合 shop-escort 再合 escort-catalog，但不強制）。
- Integration checkpoints: 兩組合併後執行 `make verify`；手動驗證 `/shop` 指令和 `/admin-panel` 護航定價頁面。
- Re-coordination trigger: 若 `ProductRepository` 方法簽名衝突或 escort-catalog 需要修改 shop-escort 已修改的檔案。
