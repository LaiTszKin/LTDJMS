# 模組說明：伺服器貨幣系統

本文件介紹 Discord 伺服器貨幣系統的主要元件與流程，協助開發者理解 `/balance`、`/currency-config`、`/adjust-balance` 等指令的實作方式。

## 1. 功能概觀

貨幣系統提供以下核心能力：

- 為每個伺服器設定獨立的貨幣名稱與圖示。
- 為每個成員在每個伺服器建立不會為負的貨幣帳戶。
- 讓管理員透過指令或管理面板調整成員餘額。
- 讓成員隨時透過 `/balance` 查詢自己的餘額。

相關指令：

- `/balance`
- `/currency-config`
- `/adjust-balance`
- `/user-panel`（以面板形式顯示餘額）
- `/admin-panel`（圖形化餘額管理）

## 2. 主要程式結構

### 2.1 領域模型（domain）

- `ltdjms.discord.currency.domain.GuildCurrencyConfig`  
  對應資料表 `guild_currency_config`，描述伺服器的貨幣名稱與圖示。

- `ltdjms.discord.currency.domain.MemberCurrencyAccount`  
  對應 `member_currency_account`，代表成員在伺服器內的貨幣帳戶，保證餘額非負。

- `ltdjms.discord.currency.domain.BalanceView`  
  用於輸出到使用者的輕量投影，通常包含：
  - 貨幣餘額
  - 貨幣名稱
  - 貨幣圖示
  - 將上述資訊組合成訊息的 `formatMessage()`。

### 2.2 資料存取層（persistence）

主要介面：

- `MemberCurrencyAccountRepository`
- `GuildCurrencyConfigRepository`

實作類別：

- `JdbcMemberCurrencyAccountRepository` / `JooqMemberCurrencyAccountRepository`
- `JdbcGuildCurrencyConfigRepository` / `JooqGuildCurrencyConfigRepository`

職責：

- 將 domain 模型（帳戶、貨幣設定）映射到 PostgreSQL 資料表。
- 提供常用操作，例如：
  - `findOrCreate(guildId, userId)`
  - `adjustBalance(guildId, userId, amount)`
  - `findByGuildIdAndUserId(guildId, userId)`

### 2.3 服務層（services）

- `BalanceService`  
  專責查詢餘額，回傳 `Result<BalanceView, DomainError>`：
  - 若帳戶不存在會自動建立（依實作而定）
  - 若伺服器尚未設定貨幣，回傳對應的 `DomainError`

- `CurrencyConfigService`  
  負責讀寫 `GuildCurrencyConfig`：
  - 允許只更新名稱或只更新圖示
  - 會做基礎驗證（例如圖示格式、長度等）

- `BalanceAdjustmentService`  
  提供三種調整方法：
  - `tryAdjustBalance(guildId, userId, delta)`：以差額加減
  - `tryAdjustBalanceTo(guildId, userId, targetBalance)`：調整至目標餘額
  - 回傳 `Result<BalanceAdjustmentResult, DomainError>`，其中 `BalanceAdjustmentResult` 包含：
    - 調整前餘額
    - 調整後餘額
    - 實際調整金額

這些服務都會使用 `Result<T, DomainError>` 表示成功或失敗，避免直接拋出業務例外。

### 2.4 指令處理器（commands）

- `BalanceCommandHandler` – `/balance`
- `CurrencyConfigCommandHandler` – `/currency-config`
- `BalanceAdjustmentCommandHandler` – `/adjust-balance`

這些 handler 全由 Dagger 注入對應的 services，並遵守以下模式：

1. 從 `SlashCommandInteractionEvent` 解析參數。
2. 呼叫對應 service，取得 `Result<*, DomainError>`。
3. 對 `Err` 情況呼叫 `BotErrorHandler.handleDomainError`。
4. 對 `Ok` 情況格式化並回覆 Discord 訊息。

## 3. `/balance` 執行流程

以下示意成員呼叫 `/balance` 時的關鍵步驟：

1. `SlashCommandListener` 依指令名稱將事件交給 `BalanceCommandHandler`。
2. `BalanceCommandHandler`：
   - 從事件中取得 `guildId` 與 `userId`。
   - 呼叫 `balanceService.tryGetBalance(guildId, userId)`。
3. `BalanceService`：
   - 透過 `MemberCurrencyAccountRepository` 查詢或建立帳戶。
   - 取得伺服器貨幣設定（`GuildCurrencyConfigRepository`）。
   - 組裝成 `BalanceView`。
4. 成功時：
   - `BalanceCommandHandler` 呼叫 `balanceView.formatMessage()` 取得字串訊息。
   - 回覆給使用者。
5. 若伺服器尚未設定貨幣或遇到其他業務錯誤：
   - service 會回傳 `Result.err(DomainError)`。
   - handler 透過 `BotErrorHandler.handleDomainError` 回覆錯誤訊息（多為 ephemeral）。

## 4. `/currency-config` 執行流程

1. 管理員呼叫 `/currency-config`，可帶入：
   - `name`：新貨幣名稱（選填）
   - `icon`：新貨幣圖示（選填）
2. `CurrencyConfigCommandHandler` 從事件取出參數：
   - 若兩者皆為 `null`：呼叫 service 取得目前設定並顯示。
   - 若至少一者非空：呼叫 `CurrencyConfigService.tryUpdateConfig`。
3. `CurrencyConfigService`：
   - 驗證名稱長度與圖示格式。
   - 更新 `GuildCurrencyConfig`，並回傳更新後的設定。
4. 成功時，handler 組裝成功訊息（包含名稱與圖示）回覆。
5. 若參數無效或發生資料庫錯誤，回傳對應 `DomainError`，由 `BotErrorHandler` 轉成錯誤訊息。

## 5. `/adjust-balance` 執行流程與驗證

1. 管理員呼叫 `/adjust-balance`，必填參數：
   - `mode`：`add` / `deduct` / `adjust`
   - `member`：目標成員
   - `amount`：調整金額或目標餘額
2. `BalanceAdjustmentCommandHandler`：
   - 驗證必要參數是否存在。
   - 將 `mode` 轉成對 `BalanceAdjustmentService` 的呼叫：
     - `add` 模式：要求 `amount > 0`，呼叫 `tryAdjustBalance(guildId, userId, amount)`。
     - `deduct` 模式：要求 `amount > 0`，呼叫 `tryAdjustBalance(guildId, userId, -amount)`。
     - `adjust` 模式：要求 `amount >= 0`，呼叫 `tryAdjustBalanceTo(guildId, userId, amount)`。
3. `BalanceAdjustmentService`：
   - 檢查調整後餘額是否為負；若會為負，回傳 `DomainError.insufficientBalance`。
   - 檢查是否超過單次調整上限。
   - 成功時回傳 `BalanceAdjustmentResult`。
4. Handler 將結果格式化成使用者可讀的訊息，例如：

   ```text
   Added 💰 500 金幣 to @成員
   New balance: 💰 1,000 金幣
   ```

## 6. 面板與貨幣系統的關聯

貨幣系統的 service 也被用在面板功能中：

- `UserPanelService`：
  - 透過 `BalanceService.tryGetBalance` 取得使用者餘額與貨幣設定。
  - 組成 `UserPanelView`，供 `/user-panel` 顯示。

- `AdminPanelService`：
  - 透過 `BalanceService` 顯示成員目前餘額。
  - 透過 `BalanceAdjustmentService` 實作管理面板中的「餘額調整」表單。

這讓面板可以重用既有業務邏輯，而不需直接操作 repository。

## 7. 開發建議

若你要在貨幣系統上新增功能（例如排行榜、批次發獎勵等），建議遵循以下原則：

1. **優先擴充 service 層，而非在 command handler 中堆疊邏輯**  
   讓 handler 保持「解析參數 → 呼叫 service → 格式化回應」的簡單模式。

2. **沿用 `Result<T, DomainError>` 模式**  
   - 預期錯誤（餘額不足、輸入無效）用 `DomainError` 表示。
   - 非預期錯誤保留為例外，統一由 `BotErrorHandler` 捕捉與回報。

3. **維持餘額「非負」的不變條件**  
   - 在 service 層集中驗證，不要在 handler 中各自檢查。
   - 新增的任何批次操作也應遵守相同約束。

4. **在新增功能前先閱讀測試**  
   - 參考 `src/test/java/ltdjms/discord/currency/unit/*` 的測試風格。
   - 盡量為新的 service 行為補上對應的單元／整合測試。

掌握以上結構與原則後，你應該可以相對順利地擴充或調整貨幣系統的行為。

