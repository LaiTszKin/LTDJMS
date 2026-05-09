# Contract: Remove legacy backend fulfillment config

- Date: 2026-04-10
- Feature: Remove legacy backend fulfillment config
- Change Name: remove-legacy-backend-fulfillment-config

## Purpose
本變更主要是本地資料模型、管理面板與設定面清理；沒有新增外部上游依賴，但必須與既有資料庫 schema 與現行付款流程保持相容。

## Dependency Records

None

理由：本 spec 不新增新的第三方 API / SDK 契約，風險集中在本地 schema、runtime config 與 UI 的一致性清理。
