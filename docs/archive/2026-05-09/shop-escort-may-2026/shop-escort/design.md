# Design: 商店頁面優化

- Date: 2026-05-09
- Feature: 商店頁面優化
- Change Name: shop-escort

## Design Goal

最小化變更現有架構，重用現有 `ShopView` / `ShopButtonHandler` / `ShopSelectMenuHandler` 模式，以 additive 方式加入排序、合併購買和搜尋功能。

## Change Summary

- Requested change: 商店商品按名稱排序、合併購買按鈕為統一流程、新增關鍵字搜尋。
- Existing baseline: 商品依 `created_at DESC` 排序；兩個獨立購買按鈕（貨幣購買 + 法幣下單）；無搜尋功能。
- Proposed design delta:
  1. SQL 排序改為 `ORDER BY name ASC`
  2. 兩個購買按鈕合併為一個「購買」→ 選商品 → 有雙價格時選支付方式 → 確認；單價格時跳過支付選擇直接走原流程
  3. 新增搜尋按鈕 → Modal 輸入關鍵字 → repository ILIKE 查詢 → 結果以商店 embed 顯示

## Scope Mapping

- Spec requirements covered: R1.1-R1.2, R2.1-R2.5, R3.1-R3.5
- Affected modules:
  - `src/main/java/ltdjms/discord/shop/services/ShopView.java`
  - `src/main/java/ltdjms/discord/shop/services/ShopService.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopCommandHandler.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopButtonHandler.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/product/domain/ProductRepository.java`
  - `src/main/java/ltdjms/discord/product/persistence/JdbcProductRepository.java`
  - `src/main/java/ltdjms/discord/product/services/ProductService.java`
- External contracts involved: None
- Coordination reference: `../coordination.md`
- Parallel implementation assumption: 可獨立實作，不依賴 escort-catalog spec

## Current Architecture

```
ShopCommandHandler.handle("/shop")
  ├── ShopService.getShopPage(guildId, page) → ProductRepository.findByGuildIdPaginated()
  │   └── SQL: ORDER BY created_at DESC
  ├── ShopView.buildShopEmbed(products, ...)
  └── ShopView.buildShopComponents(...)
      ├── Pagination buttons (prev/next)
      ├── BUTTON_PURCHASE ("💰 購買商品") — 僅有貨幣價格商品時
      └── BUTTON_FIAT_ORDER ("💳 法幣下單") — 僅有純法幣商品時

按鈕流程:
  BUTTON_PURCHASE → showPurchaseMenu() → SELECT_PURCHASE_PRODUCT
    → 商品選單選擇 → buildPurchaseConfirmEmbed() → BUTTON_CONFIRM_PURCHASE
  BUTTON_FIAT_ORDER → showFiatOrderMenu() → SELECT_FIAT_PRODUCT
    → 商品選單選擇 → defer + createFiatOnlyOrder()
```

## Proposed Architecture

```
ShopCommandHandler.handle("/shop")
  ├── ShopService.getShopPage(guildId, page) → ProductRepository.findByGuildIdPaginated()
  │   └── SQL: ORDER BY name ASC  ← 變更
  ├── ShopView.buildShopEmbed(products, ...)
  └── ShopView.buildShopComponents(...)
      ├── Pagination buttons (prev/next)
      ├── BUTTON_BUY ("🛒 購買") — 有可購商品時（合併） ← 變更
      └── BUTTON_SEARCH ("🔍 搜尋") — 有商品時（新增） ← 新增

購買流程（合併後）:
  BUTTON_BUY → showBuyMenu() → SELECT_BUY_PRODUCT（合併所有商品）
    → 商品選單選擇 →
      ├── 雙價格商品 → buildPaymentMethodChoiceEmbed()
      │     → BUTTON_PAY_WITH_CURRENCY → 現有貨幣購買確認流程
      │     └── BUTTON_PAY_WITH_FIAT → 現有法幣下單流程
      ├── 僅貨幣 → 現有貨幣購買確認流程
      └── 僅法幣 → 現有法幣下單流程（含 inflight guard）

搜尋流程（新增）:
  BUTTON_SEARCH → Modal("shop_search_modal", keyword input)
    → onModalInteraction() → shopService.searchProducts(guildId, keyword, 0)
    → buildShopEmbed(results) + 分頁按鈕 + "返回商店" 按鈕
```

## Component Changes

### Component 1: ShopView
- Responsibility: 建構商店 embed 和 action components（不變）
- Inputs: Product lists, page info
- Outputs: MessageEmbed, List<ActionRow>
- Dependencies: `DiscordComponentRenderer`, `EmbedView`, `ButtonView`
- Invariants: PAGE_SIZE = 5, DIVIDER format, embed color 0x5865F2

### Component 2: ShopButtonHandler
- Responsibility: 處理商店頁面的按鈕互動（分頁、購買、搜尋）
- Inputs: ButtonInteractionEvent
- Outputs: editMessageEmbeds / replyModal
- Dependencies: ShopService, ProductService, ShopView
- Invariants: buttonId prefix matching

### Component 3: ShopSelectMenuHandler
- Responsibility: 處理商品選單選擇和購買確認按鈕
- Inputs: StringSelectInteractionEvent, ButtonInteractionEvent
- Outputs: editMessageEmbeds / reply
- Dependencies: ProductService, BalanceService, CurrencyPurchaseService, FiatOrderService, EscortDispatchHandoffService
- Invariants: inflight key for fiat orders, idempotent handoff check

### Component 4: JdbcProductRepository
- Responsibility: 商品資料庫查詢（新增搜尋方法、修改排序）
- Inputs: guildId, keyword, page, size
- Outputs: List<Product>, long count
- Dependencies: DataSource
- Invariants: 參數化查詢防止 SQL injection

## Sequence / Control Flow

1. 使用者執行 `/shop` → `ShopCommandHandler.handle()` 取得分頁商品（已排序）
2. 商店 embed 顯示三個按鈕：上一頁、下一頁、購買、搜尋
3. 使用者點「購買」→ 顯示合併商品選單 → 選擇商品
4. 判斷商品價格類型 → 雙價格顯示支付選擇 → 單價格直走原流程
5. 使用者點「搜尋」→ Modal 彈出 → 輸入關鍵字 → ILIKE 查詢 → 顯示結果
6. 搜尋結果支援分頁和返回商店

## Data / State Impact

- Created or updated data: 無新增資料表，僅修改 SQL 排序和新增 ILIKE 查詢
- Consistency rules: 排序一致性由資料庫層保證
- Migration / rollout needs: None（無 schema 變更）

## Risk and Tradeoffs

- Key risks:
  - 合併購買按鈕後的使用者體驗需手動測試（無 UI 自動化測試）
  - SQL ILIKE 在大量商品時可能有性能問題（目前商品量級很小，風險低）
- Rejected alternatives:
  - 使用 PostgreSQL full-text search (tsvector) → 過度工程化，商品名稱簡短，ILIKE 足夠
  - 新增獨立 `/shop search` slash command → 使用者需記憶指令，按鈕方案更直覺
- Operational constraints: 無
- Cross-spec collision notes: `ProductRepository` 介面 additive-only；與 escort-catalog 無衝突

## Validation Plan

- Tests: UT（repository SQL 變更、service 層搜尋邏輯）
- Contract checks: 無外部合約
- Rollback / fallback: SQL 排序可透過 revert commit 恢復；合併按鈕為純 UI 變更，不影響資料完整性

## Open Questions
None
