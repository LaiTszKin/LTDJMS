# 開發指南：測試策略與指令

本文件說明 LTDJMS 專案中現有的測試分類、常用執行指令，以及在新增或修改功能時撰寫測試的建議。

## 1. 測試分類與目錄結構

所有測試程式碼位於：

- `src/test/java/ltdjms/discord/...`

主要依功能與測試類型拆分：

- **貨幣系統（currency）**
  - `unit/`：服務與指令 handler 的單元測試  
    例如 `BalanceServiceTest`、`CurrencyConfigServiceTest`、`BalanceCommandHandlerTest`。
  - `integration/`：包含資料庫與 JDA 模擬的整合測試  
    例如 `BalanceServiceIntegrationTest`、`CurrencyConfigCommandIntegrationTest`。
  - `contract/`：針對對外行為（如指令回應格式）的契約測試。  
  - `performance/`：針對 slash commands 的效能與延遲測試。

- **遊戲代幣與小遊戲（gametoken）**
  - `domain/`：domain 類別的行為測試（例如 `GameTokenAccountTest`）。
  - `services/`：遊戲服務與代幣服務的單元測試（例如 `DiceGame1ServiceTest`）。
  - `integration/`：Repository 與資料庫整合測試。
  - `unit/`：指令 handler 與交易服務的單元測試。

- **面板（panel）**
  - `unit/`：面板服務（`UserPanelService` 等）的單元測試。

- **共用模組（shared）**
  - `EnvironmentConfig`、`DotEnvLoader`、`Result` 等核心元件的單元測試。
  - DI 組態測試（例如 `AppComponentLoadTest`）。

## 2. 常用測試指令

所有指令皆在專案根目錄執行。

### 2.1 使用 Make

```bash
# 執行單元測試
make test

# 執行所有測試（含整合測試）
make test-integration

# 完整驗證（重新建置並跑所有測試）
make verify

# 產生 coverage 報告
make coverage
```

### 2.2 直接使用 Maven

```bash
# 單元測試
mvn test

# 全部測試 + 驗證
mvn clean verify
```

## 3. 新增功能時的測試建議流程

當你要新增功能或修改行為時，建議使用類似 TDD 的流程：

1. **先找既有測試**  
   - 在 `src/test/java` 中尋找與你修改的模組相對應的測試檔案。
   - 觀察命名慣例與使用的測試工具（例如 JUnit、Mockito）。

2. **為新行為撰寫或修改測試**  
   - 若是新增 service 行為，優先新增「單元測試」。
   - 若是新增指令或改變回應格式，可考慮：
     - 單元測試（針對 handler 的邏輯與錯誤處理）。
     - 契約測試（確認回應文字符合預期）。

3. **先讓新測試失敗**  
   - 執行 `make test` 或 `mvn test`，確認新測試確實因尚未實作而失敗。

4. **實作最小必要程式碼讓測試通過**  
   - 實作或修改對應程式碼。
   - 重複執行測試直到所有相關測試通過。

5. **必要時再補整合測試**  
   - 若新行為跨越多層（指令＋服務＋資料庫），可在 `integration/` 目錄補上整合測試。

## 4. 修改既有功能時的測試策略

### 4.1 行為不應變更的情況

例如重構、效能優化或內部實作調整，但對外行為應維持不變：

- 請確認相關的單元與整合測試都涵蓋了關鍵案例。
- 修改完成後，至少執行：

  ```bash
  make test
  make test-integration
  ```

### 4.2 行為有意義變更的情況

例如新增錯誤訊息、改變指令回應格式或調整邊界條件：

- 優先更新對應的測試，讓測試描述「新的預期行為」。
- 確認舊的測試不會錯誤地描述過時需求。

## 5. 撰寫測試時的實務提醒

- **善用測試工具與共用 util**  
  - 對 JDA 相關測試，可重用 `src/test/java/ltdjms/discord/currency/unit/JdaTestUtils.java` 等工具。

- **將預期錯誤視為正常情境測試**  
  - 由於專案使用 `Result<T, DomainError>`，請記得也測試錯誤情境（如餘額不足、輸入無效等），確保 `DomainError.Category` 使用正確。

- **維持測試命名一致性**  
  - 測試類別與方法命名盡量與現有風格一致，便於未來維護與搜尋。

## 6. 本文件與實際執行情況

此文件僅說明測試策略與指令，**不會自動執行任何測試**。  
實際開發時請依下列指令在本機或 CI 中執行測試：

- 單元測試：`make test`
- 全部測試：`make test-integration` 或 `mvn clean verify`

