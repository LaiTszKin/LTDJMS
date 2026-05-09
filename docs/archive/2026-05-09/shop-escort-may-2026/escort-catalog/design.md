# Design: 護航目錄資料庫化與價目表管理

- Date: 2026-05-09
- Feature: 護航目錄資料庫化與價目表管理
- Change Name: escort-catalog

## Design Goal

將硬編碼護航目錄遷移至資料庫，遵循現有 JDBC repository 模式，最小化對既有護航派單和商品系統的影響，並在管理面板中提供直覺的商店式 CRUD 介面。

## Change Summary

- Requested change: 護航價目從硬編碼 `EscortOrderOptionCatalog` 改為資料庫驅動，管理面板新增商店式價目表管理頁面。
- Existing baseline: `EscortOrderOptionCatalog` 硬編碼 26 個選項；`EscortOptionPricingService` 從硬編碼讀取所有選項後與公會覆蓋價合併；管理面板以 field list 方式顯示價目。
- Proposed design delta:
  1. 新增 `escort_option_catalog` 表（V028 migration），匯入現有 26 筆種子資料
  2. 新增 `EscortOptionCatalog` domain record + `EscortOptionCatalogRepository` + JDBC 實作
  3. `EscortOrderOptionCatalog` 改為代理層，讀取新 repository（保留公開 API）
  4. `EscortOptionPricingService` 改用新 repository 作為基礎資料源
  5. 管理面板新增 `AdminEscortCatalogView` — 商店式分頁 embed + CRUD Modal

## Scope Mapping

- Spec requirements covered: R1.1-R1.3, R2.1-R2.5, R3.1-R3.3, R4.1-R4.5, R5.1-R5.2
- Affected modules:
  - `src/main/resources/db/migration/V028__*.sql`
  - `src/main/java/ltdjms/discord/product/domain/EscortOptionCatalog.java` (new)
  - `src/main/java/ltdjms/discord/product/domain/EscortOptionCatalogRepository.java` (new)
  - `src/main/java/ltdjms/discord/product/persistence/JdbcEscortOptionCatalogRepository.java` (new)
  - `src/main/java/ltdjms/discord/product/domain/EscortOrderOptionCatalog.java` (refactor)
  - `src/main/java/ltdjms/discord/dispatch/services/EscortOptionPricingService.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelButtonHandler.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelViewFactory.java`
  - `src/main/java/ltdjms/discord/panel/commands/AdminPanelModalFactory.java`
  - `src/main/java/ltdjms/discord/shared/di/DispatchModule.java`
  - `src/main/java/ltdjms/discord/shared/di/AppComponent.java`
- External contracts involved: None
- Coordination reference: `../coordination.md`
- Parallel implementation assumption: 可獨立實作；與 shop-escort spec 的 ProductRepository 變更為 additive-only，無衝突

## Current Architecture

```
EscortOptionPricingService.listOptionPrices(guildId)
  ├── EscortOrderOptionCatalog.allOptions()  ← 硬編碼 26 個選項
  └── EscortOptionPriceRepository.findAllByGuildId(guildId)  ← DB 公會覆蓋
  → 合併成 List<OptionPriceView>

管理面板護航定價顯示:
  AdminPanelButtonHandler.showEscortPricingConfig()
    → EscortOptionPricingService.listOptionPrices()
    → buildEscortPricingEmbed()  ← field list 格式
```

## Proposed Architecture

```
EscortOptionPricingService.listOptionPrices(guildId)
  ├── EscortOptionCatalogRepository.findAll()  ← DB 護航目錄 (NEW)
  └── EscortOptionPriceRepository.findAllByGuildId(guildId)  ← DB 公會覆蓋 (不變)
  → 合併成 List<OptionPriceView>

管理面板護航價目表顯示:
  AdminPanelButtonHandler.showEscortCatalogList()
    → EscortOptionCatalogRepository.findAll()  ← 直接查詢目錄
    → buildEscortCatalogListEmbed()  ← 商店式編號列表 (NEW)

管理面板護航 CRUD:
  AdminPanelButtonHandler (new routes)
    → Modal (create/edit) → EscortOptionCatalogRepository.save()/update()
    → Delete confirm → EscortOptionCatalogRepository.deleteByCode()
```

## Component Changes

### Component 1: EscortOptionCatalog (new domain record)
- Responsibility: 代表 `escort_option_catalog` 表中的一筆護航選項
- Inputs: code, type, level, mapScope, target, priceTwd
- Outputs: immutable record with generated id, timestamps
- Dependencies: None
- Invariants: code unique, priceTwd > 0, 所有字串欄位非空

### Component 2: EscortOptionCatalogRepository (new interface + JDBC impl)
- Responsibility: `escort_option_catalog` 表的 CRUD 操作
- Inputs: guild-agnostic（全域目錄，不區分 guild）
- Outputs: `EscortOptionCatalog` 物件
- Dependencies: `DataSource`
- Invariants: code unique constraint enforced at DB level

### Component 3: EscortOrderOptionCatalog (refactored, eventually removed)
- Responsibility: 向後相容的查詢層，代理至 `EscortOptionCatalogRepository`
- Inputs: code string
- Outputs: `EscortOrderOption` record (unchanged)
- Dependencies: `EscortOptionCatalogRepository` (nullable for fallback)
- Invariants: 公開 API 保持不變（`findByCode`, `allOptions`, `isSupported`, `supportedCodes`）

### Component 4: EscortOptionPricingService (updated)
- Responsibility: 護航定價服務（基礎資料源從硬編碼改為 DB）
- Inputs: guildId, optionCode, priceTwd
- Outputs: `Result<OptionPriceView, DomainError>` (unchanged)
- Dependencies: `EscortOptionPriceRepository` + `EscortOptionCatalogRepository` (new)
- Invariants: 公會覆蓋價優先於預設價；價格驗證 > 0

### Component 5: AdminEscortCatalogView (new in AdminPanelViewFactory)
- Responsibility: 建構護航價目表的 embed、components 和 modal
- Inputs: List<EscortOptionCatalog>, pagination info
- Outputs: MessageEmbed, ActionRow list, Modal
- Dependencies: `PanelComponentRenderer`
- Invariants: 每頁 10 項；格式符合商店風格

## Sequence / Control Flow

1. 管理員開啟 `/admin-panel` → 主選單新增「護航價目表管理」按鈕
2. 點選後 → `showEscortCatalogList()` → 查詢 `EscortOptionCatalogRepository.findAll()`
3. 顯示商店式分頁 embed：編號 + 訂單類型 + 服務範圍 + 價格
4. 點選「新增」→ Modal（code, type, level, mapScope, target, priceTwd）→ validate → `repository.save()`
5. 點選「編輯」→ Modal 預填 → validate → `repository.update()`
6. 點選「刪除」→ 確認 → `repository.deleteByCode()`
7. 所有 CRUD 完成後自動刷新列表

## Data / State Impact

- Created or updated data: 新增 `escort_option_catalog` 表（V028 migration），26 筆種子資料
- Consistency rules: code UNIQUE；price_twd CHECK > 0；無 guild 隔離（全域共用目錄）
- Migration / rollout needs: V028 為一次性遷移；`EscortOrderOptionCatalog` 保留為過渡相容層

## Risk and Tradeoffs

- Key risks:
  - 種子資料與硬編碼不一致 — 緩解：直接從現有 `createOptions()` 方法複製資料
  - `EscortOrderOptionCatalog` 重建構依賴 DB 連線 — 緩解：保留 null-check fallback 至硬編碼
- Rejected alternatives:
  - 保留硬編碼並僅擴充 CRUD → 與需求矛盾（用戶明確要求脫離硬編碼）
  - 每個 guild 有獨立目錄 → 目前需求為全域共用目錄，公會差異僅在價格覆蓋
  - 使用 ORM (JPA/Hibernate) → 專案慣例為 JDBC，新增 ORM 會增加複雜度
- Operational constraints: 無
- Cross-spec collision notes: 與 shop-escort 無檔案衝突；`EscortOrderOptionCatalog` 修改僅由本 spec 控制

## Validation Plan

- Tests: UT（repository CRUD、pricing merge 邏輯）、IT（DB migration + repository 整合）
- Contract checks: 無外部合約
- Rollback / fallback: `EscortOrderOptionCatalog` 保留 fallback；V028 migration 可 rollback（DROP TABLE IF EXISTS）

## Open Questions
None
