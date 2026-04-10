# Tasks: Remove legacy backend fulfillment config

- Date: 2026-04-10
- Feature: Remove legacy backend fulfillment config

## **Task 1: 清理商品模型與服務層 backend URL 依賴**

對應 `R1.x`，核心目標是讓商品與護航開單配置只依賴內部資料，不再依賴外部 backend URL。

- 1. [x] 更新 Product / ProductService / repository mapping
  - 1.1 [x] 移除 `backendApiUrl` 驗證與必填條件
  - 1.2 [x] 保留 `autoCreateEscortOrder` + `escortOptionCode` 的內部校驗
  - 1.3 [x] 補齊商品建立/編輯回歸測試

## **Task 2: 清理管理面板與設定面**

對應 `R1.2`、`R2.x`，核心目標是讓 operator 不再接觸淘汰設定。

- 2. [x] 更新 admin product panel 與 config surface
  - 2.1 [x] 移除 backend URL 欄位、summary 與 modal
  - 2.2 [x] 從 `EnvironmentConfig` / `application.properties` 移除 obsolete settings
  - 2.3 [x] 更新文件、README、設定說明

## **Task 3: 安全清理 schema 與兼容性驗證**

對應 `R3.x`，核心目標是讓舊欄位移除不破壞既有資料與升級流程。

- 3. [x] 完成 migration 與兼容性測試
  - 3.1 [x] 新增 migration 調整或移除 `backend_api_url` 及相關 constraint
  - 3.2 [x] 驗證舊資料升級、讀取、更新行為
  - 3.3 [x] 補齊 migration / repository / panel regression tests
