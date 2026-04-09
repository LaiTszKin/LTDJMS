# Checklist: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

- Date: 2026-04-09
- Feature: Issue 77 Compose 自管 Nginx 入口與公開 URL 推導

## Clarification & Approval Gate
- [x] User clarification responses are recorded — `APP_PUBLIC_BASE_URL` 作為主要入口與 `ECPAY_RETURN_URL` 保留 override 已更新到 spec
- [x] Affected plans are reviewed/updated — `spec.md` / `tasks.md` / `checklist.md` / `contract.md` / `design.md`
- [x] Explicit user approval on updated specs is obtained（使用者已要求「更新計劃然後實作」）

## Behavior-to-Test Checklist

- [x] CL-77-01 `APP_PUBLIC_BASE_URL` 能推導 callback URL，且 `ECPAY_RETURN_URL` 仍可 override
  - Requirement mapping: `R1.1-R1.3`
  - Actual test case IDs: `UT-77-01`, `UT-77-02`, `UT-77-03`
  - Test level: `Unit`
  - Risk class: `regression`
  - Property/matrix focus: `normalization matrix`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `PASS`
  - Notes (optional): 覆蓋 bare host、trailing slash、explicit override

- [x] CL-77-02 Compose 會帶出 repo 管理的 Nginx ingress
  - Requirement mapping: `R2.1-R2.2`, `R3.1`
  - Actual test case IDs: `CFG-77-01`
  - Test level: `Integration`
  - Risk class: `deployment coupling`
  - Property/matrix focus: `compose rendering`
  - External dependency strategy: `docker compose config`
  - Oracle/assertion focus: `rendered config`
  - Test result: `PASS`
  - Notes (optional): 驗證 `nginx` service、network mode、mount 與 port wiring

- [x] CL-77-03 文件把 `APP_PUBLIC_BASE_URL` 視為主要設定入口，並保留 Vercel 為獨立路徑
  - Requirement mapping: `R3.2-R3.3`
  - Actual test case IDs: `DOC-77-01`
  - Test level: `Review`
  - Risk class: `regression`
  - Property/matrix focus: `docs consistency`
  - External dependency strategy: `none`
  - Oracle/assertion focus: `exact output`
  - Test result: `PASS`
  - Notes (optional): README、configuration、getting-started 需一致

## Required Hardening Records
- [x] Regression tests are added/updated for bug-prone or high-risk behavior
- [x] Property-based coverage is added/updated for changed business logic, or `N/A` is recorded with a concrete reason — `N/A`，本次為 deterministic config 推導與 Compose wiring，無新增適合 property-based 的業務規則
- [x] External services in the business logic chain are mocked/faked for scenario testing, or `N/A` is recorded with a concrete reason — `N/A`，未新增需 mock 的外部 API 呼叫鏈
- [x] Adversarial/penetration-style cases are added/updated for abuse paths and edge combinations, or `N/A` is recorded with a concrete reason — `N/A`，本次未改變授權面或外部輸入解析面；主要風險以 normalization matrix 與 Compose 組態覆蓋
- [x] Authorization, invalid transition, replay/idempotency, and concurrency risks are evaluated; uncovered items are marked `N/A` with concrete reasons — authorization / invalid transition / replay / concurrency `N/A`，本次未改動付款狀態流與授權模型
- [x] Assertions verify business outcomes and side effects/no-side-effects, not only "returns 200" or "does not throw"
- [x] Test fixtures are reproducible (fixed seed/clock/fixtures) or `N/A` is recorded with a concrete reason — `N/A`，Compose config 與 URL 推導不依賴隨機或時鐘

## E2E / Integration Decision Records

### Decision Record 1: Compose ingress wiring
- Requirement mapping: `R2.x-R3.1 / CL-77-02`
- Decision: `Cover with integration instead`
- Linked case IDs: `CFG-77-01`
- Reason: 風險在 Compose 產生的部署組態，不適合用 JUnit 模擬；`docker compose config` 足以驗證主要 wiring

### Decision Record 2: 公開 URL 推導
- Requirement mapping: `R1.x / CL-77-01`
- Decision: `Existing coverage already sufficient`
- Linked case IDs: `EnvironmentConfigTest`, `EnvironmentConfigDotEnvIntegrationTest`
- Reason: URL 推導屬 deterministic config logic，用單元測試最穩定

## Execution Summary
- [x] Unit tests: `PASS`
- [x] Regression tests: `PASS`
- [x] Property-based tests: `N/A`
- [x] Integration tests: `PASS`
- [x] E2E tests: `N/A`
- [x] External service mock scenarios: `N/A`
- [x] Adversarial/penetration-style cases: `N/A`

## Completion Records

### Completion Record 1: 規格與設計
- Requirement mapping: `R1.x-R3.x / Task 1-3 / CL-77-01~03`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: 本 spec 已收斂成「base URL 推導 + Compose Nginx ingress + 文件對齊」，不含 TLS 自動化與 Vercel 移除

### Completion Record 2: 實作與測試
- Requirement mapping: `Task 1-3`
- Completion status: `completed`
- Remaining applicable items: `None`
- Notes: `EnvironmentConfig`、`.env.example`、`docker-compose.yml`、`docker/nginx/default.conf` 與部署文件已更新；URL 推導與 Compose config 驗證通過
