# Tasks: 商店頁面優化

- Date: 2026-05-09
- Feature: 商店頁面優化

## **Task 1: 商品排序改為名稱**

Purpose: 將商店商品列表從依建立時間改為依名稱排序。
Requirements: R1.1, R1.2
Scope: `JdbcProductRepository.java`
Out of scope: 搜尋功能、購買按鈕變更

- 1. [ ] **`src/main/java/ltdjms/discord/product/persistence/JdbcProductRepository.java:findByGuildIdPaginated()`** — 將 SQL `ORDER BY created_at DESC` 改為 `ORDER BY name ASC`
  - Verify: `make test` 通過 ShopService 相關測試；手動檢查 SQL 字串變更

## **Task 2: ProductRepository 新增搜尋方法**

Purpose: 在 repository 層新增依名稱模糊搜尋的方法。
Requirements: R3.1, R3.2
Scope: `ProductRepository.java`, `JdbcProductRepository.java`
Out of scope: UI 層的搜尋按鈕和 Modal

- 1. [ ] **`src/main/java/ltdjms/discord/product/domain/ProductRepository.java`** — 新增方法簽名 `List<Product> findByGuildIdAndNameContaining(long guildId, String keyword, int page, int size)` 和 `long countByGuildIdAndNameContaining(long guildId, String keyword)`
  - Verify: 編譯通過；方法簽名與呼叫端一致

- 2. [ ] **`src/main/java/ltdjms/discord/product/persistence/JdbcProductRepository.java`** — 實作兩個新方法：`SELECT ... FROM product WHERE guild_id = ? AND name ILIKE '%' || ? || '%' ORDER BY name ASC LIMIT ? OFFSET ?` 和 `SELECT COUNT(*) FROM product WHERE guild_id = ? AND name ILIKE '%' || ? || '%'`
  - Verify: `make test` 通過

## **Task 3: ShopService 新增搜尋分頁方法**

Purpose: 在 service 層封裝搜尋邏輯，與現有 `getShopPage` 模式一致。
Requirements: R3.1, R3.2
Scope: `ShopService.java`
Out of scope: UI 層變更

- 1. [ ] **`src/main/java/ltdjms/discord/shop/services/ShopService.java`** — 新增 `searchProducts(long guildId, String keyword, int page)` 方法，回傳 `ShopPage`；關鍵字為空或 null 時回傳空結果
  - Verify: 現有 `ShopServiceTest` 通過；必要時新增搜尋測試案例

## **Task 4: 合併購買按鈕與流程**

Purpose: 將「貨幣購買」和「法幣下單」兩個按鈕合併為單一統一購買流程。
Requirements: R2.1, R2.2, R2.3, R2.4, R2.5
Scope: `ShopView.java`, `ShopButtonHandler.java`, `ShopSelectMenuHandler.java`, `ShopCommandHandler.java`
Out of scope: 搜尋功能、法幣訂單建立邏輯

- 1. [ ] **`src/main/java/ltdjms/discord/shop/services/ShopView.java`** — 移除 `BUTTON_PURCHASE`、`BUTTON_FIAT_ORDER`、`SELECT_PURCHASE_PRODUCT`、`SELECT_FIAT_PRODUCT` 常數；新增 `BUTTON_BUY = "shop_buy"` 和 `SELECT_BUY_PRODUCT = "shop_buy_select"`；`buildShopComponents()` 合併為單一「購買」按鈕；新增 `buildBuyMenu(List<Product> allProducts)` 包含所有可購買商品
  - Verify: 編譯通過；`ShopView` 公開常數與按鈕 handler 一致

- 2. [ ] **`src/main/java/ltdjms/discord/shop/services/ShopView.java`** — 新增 `buildPaymentMethodChoiceEmbed(Product product)` 方法，當商品同時有貨幣和法幣價格時顯示選擇 embed；新增 `BUTTON_PAY_WITH_CURRENCY` 和 `BUTTON_PAY_WITH_FIAT` 按鈕
  - Verify: 編譯通過；新方法回傳非 null embed 和 components

- 3. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopButtonHandler.java`** — 將 `BUTTON_PURCHASE` / `BUTTON_FIAT_ORDER` 的分歧處理改為 `BUTTON_BUY`；`showPurchaseMenu()` 改為顯示合併商品選單；新增 `handleBuyButton()` 方法
  - Verify: `make build` 通過（需更新所有引用點）

- 4. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`** — 修改 `onStringSelectInteraction()` 處理新的 `SELECT_BUY_PRODUCT`；從商品選單選擇後判斷商品價格類型：若同時有兩種價格則顯示支付方式選擇；若僅有貨幣價格則走原貨幣購買確認流程；若僅有法幣價格則走原法幣下單流程
  - Verify: 編譯通過；舊的 `SELECT_PURCHASE_PRODUCT` / `SELECT_FIAT_PRODUCT` 引用已移除

- 5. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopSelectMenuHandler.java`** — 新增 `onButtonInteraction()` 處理 `BUTTON_PAY_WITH_CURRENCY` 和 `BUTTON_PAY_WITH_FIAT` 按鈕；`BUTTON_PAY_WITH_CURRENCY` 顯示貨幣購買確認（現有邏輯）；`BUTTON_PAY_WITH_FIAT` 觸發法幣下單（現有邏輯）
  - Verify: 完整購買流程手動測試（見下方 E2E 驗證）

- 6. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopCommandHandler.java`** — 更新 `handle()` 方法使用新的合併按鈕常數；`getProductsForPurchase()` 和 `getFiatOnlyProducts()` 呼叫合併為取得所有可購買商品
  - Verify: `make build` 通過

## **Task 5: 商店搜尋 UI**

Purpose: 商店頁面新增搜尋按鈕和 Modal，輸入關鍵字後顯示搜尋結果。
Requirements: R3.3, R3.4, R3.5
Scope: `ShopView.java`, `ShopButtonHandler.java`, `ShopCommandHandler.java`（或新 Search handler）
Out of scope: repository/service 層（已在 Task 2, 3 完成）

- 1. [ ] **`src/main/java/ltdjms/discord/shop/services/ShopView.java`** — 新增 `BUTTON_SEARCH = "shop_search"` 常數；`buildShopComponents()` 在按鈕列加入「🔍 搜尋」按鈕（有商品時顯示）；新增 `buildSearchModal()` 建立搜尋 Modal（單一文字輸入欄位 "keyword"）
  - Verify: 編譯通過；按鈕和 Modal 的 component ID 與 handler 一致

- 2. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopButtonHandler.java`** — 處理 `BUTTON_SEARCH` 按鈕事件，回覆 `buildSearchModal()`
  - Verify: 點選搜尋按鈕後 Discord 彈出 Modal

- 3. [ ] **`src/main/java/ltdjms/discord/shop/commands/ShopButtonHandler.java`** 或新檔案 **`ShopSearchHandler.java`** — 處理搜尋 Modal 提交 `onModalInteraction()`：讀取 keyword，呼叫 `shopService.searchProducts()`，用 `ShopView.buildShopEmbed()` 顯示結果（附帶「返回商店」按鈕）
  - Verify: 輸入關鍵字後顯示匹配商品；無匹配時顯示「找不到符合關鍵字的商品」；分頁正常

## Notes
- Task 順序反映實作順序：Task 1 (排序) 為最小獨立變更，可最先完成；Task 2-3 (搜尋後端) 在 Task 5 (搜尋 UI) 之前；Task 4 (合併按鈕) 為最大變更，最後合併。
- 所有 task 對應 `spec.md` 的 Requirement ID。
- 每個 checkbox 必須包含具體檔案路徑、變更內容、驗證方式。
- 搜尋 Modal ID 使用 `"shop_search_modal"` 以與其他 modal handler 區分。
