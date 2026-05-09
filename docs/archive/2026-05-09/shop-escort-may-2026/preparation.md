# Preparation: 商店與護航定價優化

- Date: 2026-05-09
- Batch: shop-escort-may-2026

## Preparation Goal

建立護航目錄資料庫表（`escort_option_catalog`），作為 escort-catalog spec 的基礎設施。shop-escort spec 不依賴此準備工作，可獨立進行。

- Why this exists: escort-catalog spec 需要資料庫表來儲存護航項目，Flyway 遷移必須在實作 repository 層之前完成。
- Core business boundary: 僅建立 schema 和種子資料，不實作任何業務邏輯。
- Depends on (specs): [escort-catalog]
- Parallel work starts after: 遷移腳本通過 `make verify` 且資料庫可正常啟動。
- Out of scope: repository 介面/實作、service 層、管理面板 UI、任何業務邏輯。

## **Task P1: 建立 escort_option_catalog 資料表**

Purpose: 提供護航目錄的持久化儲存，取代硬編碼的 `EscortOrderOptionCatalog`。
Scope: `src/main/resources/db/migration/V028__create_escort_option_catalog.sql`
Out of scope: repository/service/UI 實作

- P1. [ ] **`src/main/resources/db/migration/V028__create_escort_option_catalog.sql`** — 建立 `escort_option_catalog` 表，包含欄位 `id (BIGSERIAL PK)`、`code (VARCHAR(120) UNIQUE NOT NULL)`、`type (VARCHAR(64) NOT NULL)`、`level (VARCHAR(64) NOT NULL)`、`map_scope (VARCHAR(256) NOT NULL)`、`target (VARCHAR(256) NOT NULL)`、`price_twd (BIGINT NOT NULL)`、`created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())`、`updated_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())`；加入 CHECK 約束確保 price_twd > 0；匯入現有 26 筆種子資料。
  - Verify: `make db-up && docker exec -it $(docker ps -qf "name=postgres") psql -U ltdjms -d ltdjms -c "SELECT count(*) FROM escort_option_catalog;"` 回傳 26

- P2. [ ] **`src/main/resources/db/migration/V028__create_escort_option_catalog.sql`** — 加入 `updated_at` 自動更新 trigger。
  - Verify: 執行 UPDATE 後檢查 `updated_at` 是否自動更新

## Validation

- Verification required: `make db-up` 成功啟動；查詢 `escort_option_catalog` 表有 26 筆資料。
- Expected results: Flyway 遷移成功，表結構與種子資料正確。
- Regression risks covered: 不影響現有表和查詢。

## Handoff

- Preparation commit required before parallel work: Yes（僅 escort-catalog 需要）
- Member specs assume: `escort_option_catalog` 表存在且有 26 筆種子資料。
- Member specs must not change: 遷移腳本（schema 凍結後僅可透過新遷移修改）。
- Member specs own all business behavior: Yes
- If preparation changes later: 新增 V029 遷移而非修改 V028。
