# Feature Specification: Discord API 抽象層

**Feature Branch**: `001-discord-api-abstraction`
**Created**: 2025-12-27
**Status**: Draft
**Input**: User description: "我想要將現有業務邏輯中對discord的api的呼叫抽象成一個獨立可擴展的api模塊"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 開發者能夠在業務邏輯中使用統一的 Discord API 介面 (Priority: P1)

開發者需要在不直接依賴 JDA 實作細節的情況下，實現與 Discord 的互動操作。當開發者撰寫命令處理器或服務層邏輯時，他們能夠使用抽象介面來處理 Discord 相關操作，而無需關心底層實作。

**Why this priority**: 這是最核心的需求，解決了直接依賴 JDA 導致的代碼耦合問題，為後續所有擴展和測試奠定基礎。

**Independent Test**: 可以透過實作一個簡單的命令處理器（如餘額查詢），使用新的抽象介面替換原有的 JDA 直接調用，驗證功能正常運作。

**Acceptance Scenarios**:

1. **Given** 開發者正在實作一個新的斜線命令處理器，**When** 使用抽象介面來回應使用者訊息，**Then** 訊息能正常顯示在 Discord 頻道中
2. **Given** 開發者需要發送 Embed 訊息，**When** 透過抽象介面建構並發送，**Then** Embed 能正確顯示所有欄位與元件
3. **Given** 現有代碼直接使用 JDA API，**When** 逐步遷移到抽象介面，**Then** 功能保持一致且無破壞性變更

---

### User Story 2 - 開發者能夠使用建構器模式建立 Discord 視圖元件 (Priority: P2)

開發者需要一個一致的方式來建立 Discord 的視圖元件（Embed、Button、SelectMenu、Modal），而無需直接使用 JDA 的建構器 API。

**Why this priority**: 視圖建構是 Discord 互動的核心部分，統一建構方式能提高代碼可讀性和可維護性。

**Independent Test**: 可以透過重構現有的 `UserPanelEmbedBuilder` 或 `ShopView` 類別，使用新的建構器介面，驗證生成的視圖元件與原有實作一致。

**Acceptance Scenarios**:

1. **Given** 開發者需要建立一個 Embed 訊息，**When** 使用抽象建構器設定標題、描述、欄位、顏色，**Then** 生成的 Embed 包含所有正確的屬性
2. **Given** 開發者需要建立一個帶有按鈕的訊息，**When** 使用建構器建立按鈕並組裝成 ActionRow，**Then** 按鈕在 Discord 中正常顯示且可點擊
3. **Given** 開發者需要建立 Modal 對話框，**When** 使用建構器新增輸入欄位，**Then** Modal 能正確開啟並收集使用者輸入

---

### User Story 3 - 開發者能夠抽象管理 Discord 互動 Session (Priority: P3)

開發者需要一個統一的方式來管理 Discord 互動的 Session（如使用者面板的即時更新），而無需直接處理 `InteractionHook` 的生命週期。

**Why this priority**: Session 管理是實現即時互動（如面板更新）的關鍵，抽象化後能讓業務邏輯專注於何時更新而非如何更新。

**Independent Test**: 可以透過重構 `UserPanelUpdateListener`，使用新的 Session 抽象介面來觸發面板更新，驗證更新機制正常運作。

**Acceptance Scenarios**:

1. **Given** 使用者開啟了使用者面板，**When** 業務邏輯需要更新面板顯示，**Then** 透過抽象介面觸發更新後面板能正確反映最新狀態
2. **Given** Session 註冊後長時間無活動，**When** 系統執行清理邏輯，**Then** 過期的 Session 能被正確移除
3. **Given** 使用者關閉面板或逾時，**When** 觸發 Session 失效，**Then** 後續的更新請求能正確處理不存在的 Session

---

### User Story 4 - 開發者能夠使用抽象介面處理 Discord 事件上下文 (Priority: P2)

開發者需要從 Discord 事件中提取上下文資訊（Guild ID、User ID、命令參數等），而無需直接依賴 JDA 事件類型。

**Why this priority**: 上下文提取是所有命令處理器的第一步，統一提取方式能簡化代碼並提高可測試性。

**Independent Test**: 可以透過重構 `BalanceCommandHandler`，使用新的上下文介面來提取 Guild ID 和 User ID，驗證業務邏輯正常運作。

**Acceptance Scenarios**:

1. **Given** 收到一個斜線命令事件，**When** 使用抽象介面提取 Guild ID 和 User ID，**Then** 能正確取得所有必要的識別碼
2. **Given** 命令包含可選參數，**When** 使用抽象介面獲取參數值，**Then** 能正確處理有值和 null 的情況
3. **Given** 需要獲取使用者 Mention 格式，**When** 使用抽象介面取得，**Then** 返回正確的字串格式用於訊息中

---

### Edge Cases

- 當 Discord API 回應逾時或返回錯誤時，抽象層應如何處理？
- 當 `InteractionHook` 已失效但仍嘗試更新時，系統應如何回應？
- 當 Session 已過期但仍收到更新請求時，應如何優雅處理？
- 當建構的 Embed 超過 Discord 長度限制時，應如何處理？
- 當元件（Button/SelectMenu）數量超過 Discord 限制時，應如何處理？
- 當使用者同時在多個 Guild 開啟面板時，Session 應如何區分？

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系統 MUST 提供統一的 `DiscordInteraction` 介面來處理訊息發送、編輯、Modal 開啟等操作
- **FR-002**: 系統 MUST 提供統一的 `DiscordEmbedBuilder` 介面來建構 Embed、Button、SelectMenu、Modal 等視圖元件
- **FR-003**: 系統 MUST 提供統一的 `DiscordSessionManager` 介面來管理互動 Session 的註冊、更新、失效
- **FR-004**: 系統 MUST 提供統一的 `DiscordContext` 介面來提取事件上下文資訊（Guild ID、User ID、參數等）
- **FR-005**: 系統 MUST 支援 JDA 事件類型到抽象介面的 Adapter 模式
- **FR-006**: 系統 MUST 在抽象層處理 Discord API 調用失敗的錯誤，並返回統一的錯誤格式
- **FR-007**: 系統 MUST 支援 Ephemeral 訊息（僅使用者可見）的發送
- **FR-008**: 系統 MUST 支援 Embed 的長度驗證，超過限制時自動截斷或分片
- **FR-009**: 系統 MUST 支援元件（Button/SelectMenu）的數量驗證，超過限制時自動分組
- **FR-010**: 系統 MUST 提供 Mock 實作用於單元測試，不依賴實際 Discord API
- **FR-011**: 系統 MUST 保持向後相容，現有功能遷移後無破壞性變更
- **FR-012**: 系統 MUST 支援多 Guild 的 Session 隔離，避免跨 Guild 的資料洩漏

### Quality Requirements (Per Constitution)

- **QR-001**: Implementation MUST follow Test-Driven Development (tests first, then implementation)
- **QR-002**: Code MUST achieve minimum 80% test coverage (measured by JaCoCo)
- **QR-003**: All service methods MUST return `Result<T, DomainError>` for error handling
- **QR-004**: New operations MUST include structured logging with appropriate log levels
- **QR-005**: Public APIs MUST include Javadoc documentation
- **QR-006**: Database schema changes MUST use Flyway migrations (本功能不涉及資料庫變更)

### Key Entities

- **DiscordInteraction**: 代表單一 Discord 互動的抽象介面，提供訊息發送、編輯、Modal 開啟等操作
- **DiscordEmbedBuilder**: 視圖元件建構器的抽象介面，用於建立 Embed、Button、SelectMenu、Modal
- **DiscordSessionManager**: Session 管理的抽象介面，負責註冊、更新、失效互動 Session
- **DiscordContext**: 事件上下文的抽象介面，提供 Guild ID、User ID、命令參數等資訊提取
- **DiscordAdapter**: 將 JDA 事件類型適配到抽象介面的實作類別
- **Session**: 代表單一使用者的互動會話，包含 InteractionHook 和相關元資料

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 現有命令處理器遷移到抽象介面後，所有單元測試和整合測試通過率維持 100%
- **SC-002**: 新增命令的開發時間減少 30%（透過統一的抽象介面減少重複代碼）
- **SC-003**: 單元測試執行時間減少 50%（透過 Mock 抽象介面而非 Mock JDA）
- **SC-004**: 抽象介面的方法覆蓋率達到 100%，所有公開方法都有對應的測試案例
- **SC-005**: 代碼重複率在 Discord 操作相關代碼中降低 40%（透過統一的建構器和操作方法）
- **SC-006**: 實作新的 Discord 互動功能時，開發者無需閱讀 JDA 文檔即可完成 80% 的常見操作
- **SC-007**: 抽象層的效能開銷低於 5%（與直接使用 JDA 相比）
- **SC-008**: Session 管理的記憶體使用量低於 10MB（1000 個活躍 Session）

## Assumptions

1. **JDA 版本**: 當前專案使用 JDA 作為 Discord API 客戶端，抽象層將基於 JDA 實作
2. **非破壞性遷移**: 現有功能將逐步遷移到抽象介面，過渡期間允許 JDA 直接調用與抽象介面共存
3. **測試策略**: 單元測試使用抽象介面的 Mock 實作，整合測試使用實際 JDA 實作
4. **Scope 限制**: 抽象層僅涵蓋專案中實際使用的 Discord API 操作，非全面的 JDA 抽象
5. **Session 有效期**: Session 的預設有效期限為 15 分鐘（與 Discord InteractionHook 的限制一致）
6. **錯誤處理**: 抽象層將 Discord API 錯誤轉換為統一的 `DomainError` 格式，便於業務邏輯處理
7. **向後相容**: 抽象介面的變更將遵循 Semantic Versioning，避免破壞性變更

## Dependencies

### 內部依賴

- `shared/` 模組的 `Result<T, DomainError>` 錯誤處理機制
- `shared/` 模組的日誌工具

### 外部依賴

- JDA (Java Discord API) - 當前使用的 Discord API 客戶端
- JUnit 5 - 單元測試框架
- Mockito - Mock 框架（用於測試）

### 相關模組

- `currency/commands/` - 需要遷移的命令處理器
- `panel/commands/` - 需要遷移的面板命令處理器
- `shop/commands/` - 需要遷移的商店命令處理器
- `gametoken/commands/` - 需要遷移的遊戲代幣命令處理器

## Out of Scope

以下功能明確排除於本次抽象化範圍：

1. **Voice/Audio 操作**: 目前專案未涉及 Discord 語音功能
2. **Gateway 事件監聽**: 低階的 Gateway 事件處理仍由 `SlashCommandListener` 直接處理
3. **權限管理**: Discord 權限檢查不在抽象範圍內
4. **Rate Limiting**: JDA 已內建 Rate Limiting，抽象層不重複實作
5. **Webhook 操作**: 目前專案未使用 Webhook 功能
6. **Thread 操作**: 目前專案未使用 Discord Thread 功能
