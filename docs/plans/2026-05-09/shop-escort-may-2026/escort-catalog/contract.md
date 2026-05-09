# Contract: 護航目錄資料庫化與價目表管理

- Date: 2026-05-09
- Feature: 護航目錄資料庫化與價目表管理
- Change Name: escort-catalog

## Purpose

本規格不引入新的外部依賴，所有變更限於內部模組的資料庫 schema、repository 層和管理面板 UI。

## Dependency Records

None — 本變更不涉及新的外部函式庫、SDK、API、CLI、hosted service 或平台服務。所有變更限於 PostgreSQL JDBC（現有依賴）、JDA Discord API（現有依賴）和 Flyway（現有依賴）。護航派單流程和綠界付款不受影響。
