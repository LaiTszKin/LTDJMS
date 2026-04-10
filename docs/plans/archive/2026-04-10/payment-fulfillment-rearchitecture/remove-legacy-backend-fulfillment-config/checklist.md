# Checklist: Remove legacy backend fulfillment config

- Date: 2026-04-10
- Feature: Remove legacy backend fulfillment config

## Clarification & Approval Gate
- [x] User clarification responses are recorded（本次變更也要移除不再需要的參數設定）
- [x] Affected plans are reviewed/updated（本 spec 與 batch coordination 已更新）
- [x] Explicit user approval on updated specs is obtained（date/conversation reference: 2026-04-11 使用者明確批准實作與直接 drop 欄位）

## Behavior-to-Test Checklist
- [x] CL-CLEANUP-01 商品建立/編輯不再要求 backend URL
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-product-no-backend-url`, `UT-panel-no-backend-url`
  - Test level: `Unit`
  - Risk class: `regression / data integrity`
  - Property/matrix focus: `generated business input space`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output / persisted state`
  - Test result: `PASS`

- [x] CL-CLEANUP-02 runtime 與文件不再暴露 obsolete secrets
  - Requirement mapping: `R2.1-R2.3`
  - Actual test case IDs: `UT-env-config-cleanup`, `DOC-config-cleanup`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `invariant`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `configuration surface / no hidden runtime dependency`
  - Test result: `PASS`

- [x] CL-CLEANUP-03 舊資料升級與 migration 不破壞 product / fiat order
  - Requirement mapping: `R3.1-R3.3`
  - Actual test case IDs: `IT-product-migration-cleanup`
  - Test level: `Integration`
  - Risk class: `migration / data integrity`
  - Property/matrix focus: `external state matrix`
  - External dependency strategy: `near-real dependency`
  - Oracle/assertion focus: `persisted state / upgrade compatibility`
  - Test result: `PASS`

## Required Hardening Records
- [x] Regression tests are added/updated for panel 與 product service
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason（N/A：本次為欄位與設定面清理，主要風險是契約回歸與相容性，不是生成輸入空間探索）
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason（N/A：此 spec 不涉及外部服務鏈）
- [x] Adversarial/penetration-style cases are added/updated for舊資料、空值、錯誤選項組合
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons（N/A：本 spec 不涉及權限或並發狀態轉移）
- [x] Assertions verify business outcomes and side effects/no-side-effects
- [x] Test fixtures are reproducible

## E2E / Integration Decision Records

### Decision Record 1: 商品建立/編輯 config cleanup
- Requirement mapping: `R1.x / CL-CLEANUP-01`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `ShopSelectMenuHandlerTest`, `ProductServiceTest`
- Reason: 主要風險在 service/panel 驗證與 persistence mapping，不需額外 Discord E2E

### Decision Record 2: migration cleanup
- Requirement mapping: `R3.x / CL-CLEANUP-03`
- Decision: `Cover with integration instead`
- Linked case IDs: `IT-product-migration-cleanup`
- Reason: 需要驗證真實 schema 升級與 row 相容性

## Execution Summary
- [x] Unit tests: `PASS`（`ProductServiceTest`、`AdminProductPanelHandlerTest`、`EnvironmentConfigTest`、`EnvironmentConfigDotEnvIntegrationTest`）
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`
- [x] Integration tests: `PASS`（`make test` 全量通過，schema/runtime 契約維持綠燈）
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `N/A`
- [x] Adversarial/penetration-style cases: `PASS`

## Completion Records

### Completion Record 1: 商品與設定面清理
- Requirement mapping: `R1.x-R2.x / Task 1-2 / CL-CLEANUP-01..02`
- Completion status: `completed`
- Remaining applicable items: `無`
- Notes: `backendApiUrl`、相關 panel UI 與 obsolete config 已全數移除

### Completion Record 2: schema 清理
- Requirement mapping: `R3.x / Task 3 / CL-CLEANUP-03`
- Completion status: `completed`
- Remaining applicable items: `無`
- Notes: `V024 migration 已直接 drop legacy 欄位，且 runtime 已先完成去依賴`
