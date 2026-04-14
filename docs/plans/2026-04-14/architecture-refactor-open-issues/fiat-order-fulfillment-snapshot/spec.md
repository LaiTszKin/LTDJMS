# Spec: Fiat order fulfillment snapshot

- Date: 2026-04-14
- Feature: Fiat order fulfillment snapshot
- Owner: Codex

## Goal
讓法幣訂單在建單當下就保存完整履約契約快照，避免付款成功後再依賴 mutable `product` 主檔決定實際發獎與護航行為。

## Scope

### In Scope
- 擴充 `fiat_order` 的履約快照欄位與建立流程
- 重構 post-payment worker，改以 order snapshot replay 履約
- 對齊商品刪除 / 編輯後既有待付款訂單的履約語義、測試與文件

### Out of Scope
- 重新設計整個 product schema 或 reward domain
- 變更 `CurrencyPurchaseService` 的履約模型
- 一次性解決未付款逾期狀態（由 `fiat-order-expiry-lifecycle` 處理）

## Functional Behaviors (BDD)

### Requirement 1: 建單時必須保存完整履約快照
**GIVEN** 使用者成功建立一筆法幣訂單  
**AND** 商品當下具有獎勵與護航相關設定  
**WHEN** `FiatOrderService` 寫入 `fiat_order`  
**THEN** 訂單必須保存後續履約所需的完整契約快照  
**AND** 付款完成後不得再需要回讀 mutable `product` 主檔才能知道要履約什麼

**Requirements**:
- [ ] R1.1 `fiat_order` 必須保存 reward type / amount、auto escort flag、escort option code 與必要顯示快照
- [ ] R1.2 建單時若缺少履約必要欄位，應在建單當下失敗，而不是延後到 paid worker 才爆炸
- [ ] R1.3 快照欄位語意必須明確區分「顯示用 product name」與「履約用 contract fields」

### Requirement 2: Post-payment worker 必須只 replay order snapshot
**GIVEN** 某筆法幣訂單已轉成 `PAID`  
**WHEN** post-payment worker 執行履約  
**THEN** worker 必須只依賴 order 自身快照欄位決定發獎與護航交接  
**AND** 商品主檔被編輯或刪除不得改寫既有訂單的履約結果

**Requirements**:
- [ ] R2.1 worker 不可再以 `productService.getProduct(order.productId())` 作為履約真相來源
- [ ] R2.2 商品已刪除時，只要訂單快照完整，既有已付款訂單仍必須可完成履約
- [ ] R2.3 snapshot replay 與 buyer/admin/reward/escort side effect 的完成標記必須保持冪等

### Requirement 3: 文件與測試必須反映「契約快照」語意
**GIVEN** 此次重構改變了法幣訂單與商品主檔的責任邊界  
**WHEN** 文件、測試與 migration 一起更新  
**THEN** 都必須明確描述 `fiat_order` 為履約契約 owner  
**AND** 不可再保留「付款後讀 live product」的假設

**Requirements**:
- [ ] R3.1 單元 / 整合測試必須覆蓋商品編輯與刪除後付款的 replay case
- [ ] R3.2 文件必須說明法幣建單後的履約快照不可被商品後續修改覆寫
- [ ] R3.3 schema / repository / domain model 命名必須讓維護者可直接辨識哪些欄位是 contract snapshot

## Error and Edge Cases
- [ ] 商品在建單與付款之間被刪除時，已付款訂單仍需可完成履約
- [ ] reward 與 escort 同時存在時，snapshot 欄位必須能完整表示兩者，而不是只支援其一
- [ ] callback 重送或 worker 重跑時，snapshot replay 不得重複發獎或重複交接 dispatch
- [ ] 舊資料的 migration/backfill 需定義清楚：不可默默以 null snapshot 進入 paid worker
- [ ] 顯示名稱更新不應改寫已建立訂單的履約結果，但仍可考慮是否保留原商品名稱作為 buyer-facing snapshot

## Clarification Questions
None

## References
- Official docs:
  - None（此次變更是 repo 內部資料模型與履約邊界重構）
- Related code files:
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderService.java`
  - `src/main/java/ltdjms/discord/shop/domain/FiatOrder.java`
  - `src/main/java/ltdjms/discord/shop/services/FiatOrderPostPaymentWorker.java`
  - `src/main/java/ltdjms/discord/product/services/ProductService.java`
