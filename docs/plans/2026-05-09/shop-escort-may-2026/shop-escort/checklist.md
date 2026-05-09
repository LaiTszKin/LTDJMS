# Checklist: 商店頁面優化

- Date: 2026-05-09
- Feature: 商店頁面優化

## Usage Notes

- Test result values: `PASS / FAIL / BLOCKED / NOT RUN / N/A`.

## Clarification & Approval Gate

- [ ] Clarification responses recorded — N/A（需求已在對話中確認）
- [ ] Affected plans updated after clarification — N/A
- [ ] Explicit approval obtained (date/ref: [to be filled]).

## Behavior-to-Test Checklist

- [ ] CL-01: 商品按名稱排序顯示 — R1.1 → TU-ShopSort-01 — Result: `NOT RUN`
- [ ] CL-02: 管理面板商品列表不受排序變更影響 — R1.2 → TU-ShopSort-02 — Result: `NOT RUN`
- [ ] CL-03: 統一購買按鈕顯示（有可購商品時）— R2.1, R2.2 → TU-ShopBuy-01 — Result: `NOT RUN`
- [ ] CL-04: 商品選單合併所有可購商品 — R2.3 → TU-ShopBuy-02 — Result: `NOT RUN`
- [ ] CL-05: 雙價格商品顯示支付方式選擇 — R2.4 → TU-ShopBuy-03 — Result: `NOT RUN`
- [ ] CL-06: 僅貨幣價格商品直接顯示購買確認 — R2.4 → TU-ShopBuy-04 — Result: `NOT RUN`
- [ ] CL-07: 僅法幣價格商品直接觸發法幣下單 — R2.4 → TU-ShopBuy-05 — Result: `NOT RUN`
- [ ] CL-08: 搜尋按鈕彈出 Modal — R3.3, R3.4 → TU-ShopSearch-01 — Result: `NOT RUN`
- [ ] CL-09: 關鍵字搜尋回傳匹配商品 — R3.1, R3.2, R3.5 → TU-ShopSearch-02 — Result: `NOT RUN`
- [ ] CL-10: 無匹配商品時顯示提示 — R3.5 → TU-ShopSearch-03 — Result: `NOT RUN`
- [ ] CL-11: 搜尋結果分頁 — R3.5 → TU-ShopSearch-04 — Result: `NOT RUN`
- [ ] CL-12: 空關鍵字顯示提示 — edge case → TU-ShopSearch-05 — Result: `NOT RUN`

## Hardening Checklist

- [ ] Regression tests for bug-prone/high-risk behavior — Existing `ShopServiceTest`, `ShopButtonHandler` test patterns 保留
- [ ] Unit drift checks for non-trivial tasks — Task 2 (SQL LIKE), Task 3 (search), Task 4 (payment routing)
- [ ] Property-based coverage for business logic — N/A（商店 UI 為流程導向，非計算密集型）
- [ ] External services mocked/faked — N/A（無新增外部依賴）
- [ ] Adversarial cases for abuse paths — SQL injection via keyword 已由 PreparedStatement 參數化防範
- [ ] Authorization, idempotency, concurrency risks evaluated — 商店按鈕已使用 `setEphemeral(true)`，法幣下單已使用 inflight key
- [ ] Assertions verify outcomes/side-effects, not just "returns 200".
- [ ] Fixtures reproducible (fixed seed/clock) — N/A（無隨機性）

## E2E / Integration Decisions

- [ ] 購買流程完整測試：從 `/shop` → 選商品 → 選支付方式 → 確認購買 / 法幣下單
- [ ] 搜尋流程完整測試：從 `/shop` → 搜尋按鈕 → 輸入關鍵字 → 查看結果 → 分頁
- [ ] 現有法幣付款回呼和排程器不受影響 — 僅變更 UI 觸發路徑

## Execution Summary

- [ ] Unit: `NOT RUN`
- [ ] Regression: `NOT RUN`
- [ ] Property-based: `N/A`
- [ ] Integration: `NOT RUN`
- [ ] E2E: `NOT RUN`
- [ ] Mock scenarios: `NOT RUN`
- [ ] Adversarial: `N/A`

## Completion Records

- [ ] 商店排序: pending — Remaining: None
- [ ] 購買按鈕合併: pending — Remaining: None
- [ ] 搜尋功能: pending — Remaining: None
