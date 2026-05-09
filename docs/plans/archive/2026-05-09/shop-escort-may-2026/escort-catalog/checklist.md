# Checklist: 護航目錄資料庫化與價目表管理

- Date: 2026-05-09
- Feature: 護航目錄資料庫化與價目表管理

## Usage Notes

- Test result values: `PASS / FAIL / BLOCKED / NOT RUN / N/A`.

## Clarification & Approval Gate

- [ ] Clarification responses recorded — N/A（需求已在對話中確認）
- [ ] Affected plans updated after clarification — N/A
- [ ] Explicit approval obtained (date/ref: [to be filled]).

## Behavior-to-Test Checklist

- [ ] CL-01: 護航目錄表建立並有 26 筆種子資料 — R1.1, R1.2 → TU-CatDB-01 — Result: `NOT RUN`
- [ ] CL-02: 從 DB 讀取所有護航選項 — R2.4 → TU-CatDB-02 — Result: `NOT RUN`
- [ ] CL-03: 公會覆蓋價格合併正確顯示 — R2.4 → TU-CatPrice-01 — Result: `NOT RUN`
- [ ] CL-04: 管理面板顯示商店式價目表 — R3.1, R3.2 → TU-CatUI-01 — Result: `NOT RUN`
- [ ] CL-05: 價目表支援分頁 — R3.3 → TU-CatUI-02 — Result: `NOT RUN`
- [ ] CL-06: 新增護航項目 — R4.1 → TU-CatCRUD-01 — Result: `NOT RUN`
- [ ] CL-07: 編輯護航項目 — R4.2 → TU-CatCRUD-02 — Result: `NOT RUN`
- [ ] CL-08: 刪除護航項目 — R4.3 → TU-CatCRUD-03 — Result: `NOT RUN`
- [ ] CL-09: 新增時 code 唯一性驗證 — R4.5 → TU-CatCRUD-04 — Result: `NOT RUN`
- [ ] CL-10: 新增時必填欄位驗證 — R4.4 → TU-CatCRUD-05 — Result: `NOT RUN`
- [ ] CL-11: 現有護航派單和商品接入設定不受影響 — R5.2 → TU-CatCompat-01 — Result: `NOT RUN`

## Hardening Checklist

- [ ] Regression tests for bug-prone/high-risk behavior — 現有 `EscortDispatchHandoffServiceTest`, `EscortOptionPricingService` 相關測試保留並更新
- [ ] Unit drift checks for non-trivial tasks — Task 1 (repository SQL), Task 3 (pricing merge logic)
- [ ] Property-based coverage for business logic — N/A（CRUD 操作為主）
- [ ] External services mocked/faked — N/A（無外部依賴）
- [ ] Adversarial cases for abuse paths — SQL injection 由 PreparedStatement 防範；非管理員權限檢查
- [ ] Authorization, idempotency, concurrency risks evaluated — 管理面板僅限管理員存取；CRUD 操作無並發特殊考量
- [ ] Assertions verify outcomes/side-effects, not just "returns 200".
- [ ] Fixtures reproducible — 資料庫測試用 Testcontainers 或 H2

## E2E / Integration Decisions

- [ ] 整合測試：從頭到尾新增護航項目、查詢、編輯、刪除的完整流程
- [ ] 現有整合測試確保不受影響（`make test-integration`）
- [ ] 手動驗證：管理面板 UI 和商店 `/shop` 指令不互相干擾

## Execution Summary

- [ ] Unit: `NOT RUN`
- [ ] Regression: `NOT RUN`
- [ ] Property-based: `N/A`
- [ ] Integration: `NOT RUN`
- [ ] E2E: `NOT RUN`
- [ ] Mock scenarios: `NOT RUN`
- [ ] Adversarial: `NOT RUN`

## Completion Records

- [ ] Domain + Repository: pending — Remaining: None
- [ ] Service 層重構: pending — Remaining: None
- [ ] 管理面板顯示: pending — Remaining: None
- [ ] CRUD 操作: pending — Remaining: None
