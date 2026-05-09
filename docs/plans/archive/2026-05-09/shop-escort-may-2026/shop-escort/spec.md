# Spec: 商店頁面優化

- Date: 2026-05-09
- Feature: 商店頁面優化
- Owner: [To be filled]

## Goal

讓一般成員在商店中按名稱排序瀏覽商品、統一購買流程，並支援關鍵字搜尋快速找到商品。

## Scope

### In Scope
- 商店商品按名稱字母排序顯示
- 合併貨幣購買與法幣下單為單一購買流程
- 商店頁面新增關鍵字搜尋按鈕與 Modal

### Out of Scope
- 商品管理面板的變更
- 護航定價相關功能
- 法幣付款流程的變更（綠界 API 互動保持不變）
- 商品兌換碼功能

## Functional Behaviors (BDD)

### Requirement 1: 商品按名稱排序
**GIVEN** guild 中有商品 A「蘋果」、商品 B「香蕉」、商品 C「櫻桃」
**AND** 商品以不同時間建立
**WHEN** 使用者在 guild 中執行 `/shop` 指令
**THEN** 商品依名稱字母順序顯示（櫻桃、香蕉、蘋果按中文筆劃或字母）
**AND** 分頁功能正常運作

**Requirements**:
- [ ] R1.1 `JdbcProductRepository.findByGuildIdPaginated()` 將 `ORDER BY created_at DESC` 改為 `ORDER BY name ASC`
- [ ] R1.2 管理面板商品列表保持依建立時間排序（不受影響）

### Requirement 2: 合併購買按鈕
**GIVEN** 商店中有可貨幣購買的商品和可法幣下單的商品
**WHEN** 使用者執行 `/shop` 指令
**THEN** 只顯示一個「購買」按鈕（取代原有的「購買商品」和「法幣下單」兩個按鈕）
**AND** 點選後彈出商品選擇下拉選單（包含所有可購買商品，不分支付方式）

**GIVEN** 使用者從商品選單中選擇了一個商品
**WHEN** 該商品同時有貨幣價格和法幣價格
**THEN** 顯示確認 embed，包含商品資訊和兩種支付方式的選項按鈕（「貨幣購買」/「法幣下單」）

**GIVEN** 使用者從商品選單中選擇了一個商品
**WHEN** 該商品僅有貨幣價格
**THEN** 直接顯示貨幣購買確認 embed（現有邏輯）
**AND** 顯示「確認購買」和「取消」按鈕

**GIVEN** 使用者從商品選單中選擇了一個商品
**WHEN** 該商品僅有法幣價格
**THEN** 直接觸發法幣下單流程（現有邏輯）

**Requirements**:
- [ ] R2.1 `ShopView` 移除 `BUTTON_PURCHASE` 和 `BUTTON_FIAT_ORDER`，新增統一的 `BUTTON_BUY`
- [ ] R2.2 `ShopView.buildShopComponents()` 合併為單一購買按鈕（有商品時即顯示）
- [ ] R2.3 `ShopView.buildPurchaseMenu()` 合併所有可購買商品（不分貨幣/法幣），選單選項顯示價格資訊
- [ ] R2.4 `ShopSelectMenuHandler` 新增支付方式選擇邏輯：商品同時有兩種價格時顯示支付方式選擇按鈕
- [ ] R2.5 `ShopButtonHandler` 處理新的 `shop_buy` 按鈕事件

### Requirement 3: 關鍵字搜尋
**GIVEN** 商店中有多個商品
**WHEN** 使用者點選商店頁面的「搜尋」按鈕
**THEN** 彈出 Modal 要求輸入關鍵字
**AND** 提交後使用 `LIKE '%keyword%'` 查詢資料庫
**AND** 結果以商店 embed 格式（含編號、名稱、價格等）顯示
**AND** 無匹配商品時顯示「找不到符合關鍵字的商品」

**GIVEN** 搜尋結果超過 5 筆
**WHEN** 結果顯示在第一頁
**THEN** 支援分頁瀏覽

**Requirements**:
- [ ] R3.1 `ProductRepository` 新增 `findByGuildIdAndNameContaining(long guildId, String keyword, int page, int size)` 和 `countByGuildIdAndNameContaining(long guildId, String keyword)` 方法
- [ ] R3.2 `JdbcProductRepository` 實作 SQL `WHERE guild_id = ? AND name ILIKE '%' || ? || '%' ORDER BY name ASC`
- [ ] R3.3 `ShopView` 新增搜尋按鈕 `BUTTON_SEARCH` 和搜尋 Modal
- [ ] R3.4 `ShopButtonHandler` 處理搜尋按鈕事件，彈出 Modal
- [ ] R3.5 `ShopCommandHandler` 或新 handler 處理搜尋 Modal 提交，顯示搜尋結果

## Error and Edge Cases
- [ ] 商店無任何商品時，「購買」和「搜尋」按鈕均不顯示
- [ ] 搜尋關鍵字為空或僅空白時，顯示提示而非執行查詢
- [ ] 搜尋關鍵字包含 SQL 特殊字元（`%`, `_`）時，使用參數化查詢安全處理（LIKE 自動轉義）
- [ ] 分頁邊界：搜尋結果為 0 筆時不顯示分頁按鈕
- [ ] 同時有貨幣和法幣價格的商品在購買流程中正確顯示兩種支付選項
- [ ] 非 guild 環境（DM）使用 `/shop` 時顯示錯誤提示

## Clarification Questions
None — 需求已在對話中確認。

## References
- Official docs:
  - JDA 5.x: `StringSelectMenu`, `Button`, `Modal` API
  - PostgreSQL ILIKE: https://www.postgresql.org/docs/current/functions-matching.html
- Related code files:
  - `src/main/java/ltdjms/discord/shop/commands/ShopCommandHandler.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopButtonHandler.java`
  - `src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`
  - `src/main/java/ltdjms/discord/shop/services/ShopView.java`
  - `src/main/java/ltdjms/discord/shop/services/ShopService.java`
  - `src/main/java/ltdjms/discord/product/domain/ProductRepository.java`
  - `src/main/java/ltdjms/discord/product/persistence/JdbcProductRepository.java`
  - `src/main/java/ltdjms/discord/product/services/ProductService.java`
