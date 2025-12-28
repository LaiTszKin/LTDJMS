---

description: "Task list for Discord API 抽象層 implementation"
---

# Tasks: Discord API 抽象層

**Input**: Design documents from `/specs/001-discord-api-abstraction/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/discord-api-contracts.java

**Tests**: 這個功能明確要求 TDD 方法（Constitution 原則 I），每個實作前必須先撰寫測試

**Organization**: 任務按使用者故事組織，實現每個故事的獨立實施和測試

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可並行執行（不同檔案，無依賴關係）
- **[Story]**: 此任務屬於哪個使用者故事（US1, US2, US3, US4）
- 描述中包含確切的檔案路徑

## Path Conventions

Java 專案結構：
- **Domain 抽象介面**: `src/main/java/ltdjms/discord/discord/domain/`
- **JDA 實作**: `src/main/java/ltdjms/discord/discord/services/`
- **Mock 實作**: `src/main/java/ltdjms/discord/discord/mock/`
- **Adapter**: `src/main/java/ltdjms/discord/discord/adapter/`
- **測試**: `src/test/java/ltdjms/discord/discord/`

---

## Phase 1: Setup (共用基礎設施)

**Purpose**: 專案初始化和基本結構建立

- [X] T001 建立 discord 模組目錄結構 src/main/java/ltdjms/discord/discord/{domain,services,mock,adapter}
- [X] T002 [P] 建立測試目錄結構 src/test/java/ltdjms/discord/discord/{domain,services,mock,adapter}
- [X] T003 [P] 在 shared/di/ 建立 DiscordModule.java 骨架（Dagger 2 模組）

**Checkpoint**: ✅ Setup 完成

---

## Phase 2: Foundational (阻斷性前置條件)

**Purpose**: 核心基礎設施，必須在任何使用者故事實施前完成

**⚠️ CRITICAL**: 此階段完成前無法開始任何使用者故事工作

- [X] T004 [P] 建立 EmbedView 值物件 in src/main/java/ltdjms/discord/discord/domain/EmbedView.java
- [X] T005 [P] 建立 ButtonView 值物件 in src/main/java/ltdjms/discord/discord/domain/ButtonView.java
- [X] T006 [P] 建立 DiscordError 錯誤類別 in src/main/java/ltdjms/discord/discord/domain/DiscordError.java
- [X] T007 [P] 擴展 DomainError 支援 Discord 錯誤類別 in src/main/java/ltdjms/discord/shared/DomainError.java

**Checkpoint**: ✅ 基礎設施就緒 - 可開始使用者故事實施

---

## Phase 3: User Story 1 - 開發者能夠在業務邏輯中使用統一的 Discord API 介面 (Priority: P1) 🎯 MVP

**Goal**: 提供統一的 Discord 互動回應抽象介面（DiscordInteraction），讓業務邏輯不直接依賴 JDA

**Independent Test**: 實作一個簡單的餘額查詢命令處理器，使用新的抽象介面替換原有的 JDA 直接調用，驗證功能正常運作

### Tests for User Story 1 (TDD - 必須先失敗)

> **NOTE: 這些測試必須先撰寫並確保失敗，然後再實作功能**

- [X] T008 [P] [US1] DiscordInteraction 介面契約測試 in src/test/java/ltdjms/discord/discord/domain/DiscordInteractionTest.java
- [X] T009 [P] [US1] JdaDiscordInteraction 實作單元測試 in src/test/java/ltdjms/discord/discord/services/JdaDiscordInteractionTest.java
- [X] T010 [P] [US1] MockDiscordInteraction 實作單元測試 in src/test/java/ltdjms/discord/discord/mock/MockDiscordInteractionTest.java

### Implementation for User Story 1

- [X] T011 [US1] 建立 DiscordInteraction 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordInteraction.java
- [X] T012 [US1] 實作 JdaDiscordInteraction in src/main/java/ltdjms/discord/discord/services/JdaDiscordInteraction.java
- [X] T013 [US1] 實作 MockDiscordInteraction in src/main/java/ltdjms/discord/discord/mock/MockDiscordInteraction.java
- [X] T014 [US1] 建立 SlashCommandAdapter in src/main/java/ltdjms/discord/discord/adapter/SlashCommandAdapter.java
- [X] T015 [US1] 遷移 BalanceCommandHandler 使用抽象介面 in src/main/java/ltdjms/discord/currency/commands/BalanceCommandHandler.java
- [X] T016 [US1] 在 DiscordModule 註冊 DiscordInteraction 相關元件 in src/main/java/ltdjms/discord/shared/di/DiscordModule.java

**Checkpoint**: ✅ User Story 1 已完全功能化且可獨立測試

---

## Phase 4: User Story 4 - 開發者能夠使用抽象介面處理 Discord 事件上下文 (Priority: P2)

**Goal**: 提供統一的 Discord 事件上下文提取介面（DiscordContext）

**Independent Test**: 重構 BalanceCommandHandler，使用新的上下文介面來提取 Guild ID 和 User ID，驗證業務邏輯正常運作

> **NOTE: 調整順序說明**: User Story 4 在 User Story 2 之前，因為 DiscordContext 是所有命令處理器的基礎依賴

### Tests for User Story 4 (TDD - 必須先失敗)

- [X] T017 [P] [US4] DiscordContext 介面契約測試 in src/test/java/ltdjms/discord/discord/domain/DiscordContextTest.java
- [X] T018 [P] [US4] JdaDiscordContext 實作單元測試 in src/test/java/ltdjms/discord/discord/services/JdaDiscordContextTest.java
- [X] T019 [P] [US4] MockDiscordContext 實作單元測試 in src/test/java/ltdjms/discord/discord/mock/MockDiscordContextTest.java

### Implementation for User Story 4

- [X] T020 [US4] 建立 DiscordContext 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordContext.java
- [X] T021 [US4] 實作 JdaDiscordContext in src/main/java/ltdjms/discord/discord/services/JdaDiscordContext.java
- [X] T022 [US4] 實作 MockDiscordContext in src/main/java/ltdjms/discord/discord/mock/MockDiscordContext.java
- [X] T023 [US4] 更新 SlashCommandAdapter 支援 DiscordContext in src/main/java/ltdjms/discord/discord/adapter/SlashCommandAdapter.java
- [X] T024 [US4] 更新 BalanceCommandHandler 使用 DiscordContext in src/main/java/ltdjms/discord/currency/commands/BalanceCommandHandler.java
- [X] T025 [US4] 在 DiscordModule 註冊 DiscordContext 相關元件 in src/main/java/ltdjms/discord/shared/di/DiscordModule.java

**Checkpoint**: User Stories 1 和 4 應獨立運作並協同工作

---

## Phase 5: User Story 2 - 開發者能夠使用建構器模式建立 Discord 視圖元件 (Priority: P2)

**Goal**: 提供統一的 Discord 視圖元件建構器（DiscordEmbedBuilder）

**Independent Test**: 重構 UserPanelEmbedBuilder 或 ShopView 類別，使用新的建構器介面，驗證生成的視圖元件與原有實作一致

### Tests for User Story 2 (TDD - 必須先失敗)

- [X] T026 [P] [US2] DiscordEmbedBuilder 介面契約測試（含長度限制驗證） in src/test/java/ltdjms/discord/discord/domain/DiscordEmbedBuilderTest.java
- [X] T027 [P] [US2] JdaDiscordEmbedBuilder 實作單元測試 in src/test/java/ltdjms/discord/discord/services/JdaDiscordEmbedBuilderTest.java
- [X] T028 [P] [US2] MockDiscordEmbedBuilder 實作單元測試 in src/test/java/ltdjms/discord/discord/mock/MockDiscordEmbedBuilderTest.java

### Implementation for User Story 2

- [X] T029 [US2] 建立 DiscordEmbedBuilder 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordEmbedBuilder.java
- [X] T030 [US2] 實作 JdaDiscordEmbedBuilder（含長度限制處理） in src/main/java/ltdjms/discord/discord/services/JdaDiscordEmbedBuilder.java
- [X] T031 [US2] 實作 MockDiscordEmbedBuilder in src/main/java/ltdjms/discord/discord/mock/MockDiscordEmbedBuilder.java
- [X] T032 [US2] 重構 UserPanelEmbedBuilder 使用 DiscordEmbedBuilder in src/main/java/ltdjms/discord/panel/services/UserPanelEmbedBuilder.java
- [X] T033 [US2] 在 DiscordModule 註冊 DiscordEmbedBuilder 相關元件 in src/main/java/ltdjms/discord/shared/di/DiscordModule.java

**Checkpoint**: ✅ User Story 2 已完全功能化且可獨立測試

---

## Phase 6: User Story 3 - 開發者能夠抽象管理 Discord 互動 Session (Priority: P3)

**Goal**: 提供統一的 Discord Session 管理介面（DiscordSessionManager）

**Independent Test**: 重構 AdminPanelSessionManager，使用新的 Session 抽象介面來管理面板，驗證 Session 管理機制正常運作

### Tests for User Story 3 (TDD - 必須先失敗)

- [X] T034 [P] [US3] DiscordSessionManager 介面契約測試（含過期邏輯） in src/test/java/ltdjms/discord/discord/domain/DiscordSessionManagerTest.java
- [X] T035 [P] [US3] InteractionSessionManager 實作單元測試 in src/test/java/ltdjms/discord/discord/services/InteractionSessionManagerTest.java
- [X] T036 [P] [US3] Session 過期與清理邏輯整合測試 in src/test/java/ltdjms/discord/discord/services/InteractionSessionManagerIntegrationTest.java

### Implementation for User Story 3

- [X] T037 [US3] 建立 SessionType 枚舉 in src/main/java/ltdjms/discord/discord/domain/SessionType.java
- [X] T038 [US3] 建立 Session 記錄類別 in src/main/java/ltdjms/discord/discord/domain/DiscordSessionManager.java (作為內部 record)
- [X] T039 [US3] 建立 DiscordSessionManager 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordSessionManager.java
- [X] T040 [US3] 實作 InteractionSessionManager（泛型化） in src/main/java/ltdjms/discord/discord/services/InteractionSessionManager.java
- [X] T041 [US3] 重構 AdminPanelSessionManager 使用抽象介面 in src/main/java/ltdjms/discord/panel/services/AdminPanelSessionManager.java
- [X] T042 [US3] 建立 ButtonInteractionAdapter in src/main/java/ltdjms/discord/discord/adapter/ButtonInteractionAdapter.java
- [X] T043 [US3] 更新 UserPanelButtonHandler 註冊說明 in src/main/java/ltdjms/discord/panel/commands/UserPanelButtonHandler.java
- [X] T044 [US3] 在 DiscordModule 註冊 DiscordSessionManager 相關元件 in src/main/java/ltdjms/discord/shared/di/DiscordModule.java

**Checkpoint**: ✅ User Story 3 已完全功能化且可獨立測試

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 影響多個使用者故事的改進

- [X] T045 [P] 建立 ModalInteractionAdapter 支援 Modal 事件 in src/main/java/ltdjms/discord/discord/adapter/ModalInteractionAdapter.java
- [X] T046 [P] 建立 DiscordButtonEvent 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordButtonEvent.java
- [X] T047 [P] 建立 DiscordModalEvent 介面 in src/main/java/ltdjms/discord/discord/domain/DiscordModalEvent.java
- [X] T048 [P] 更新 BotErrorHandler 支援 DiscordError in src/main/java/ltdjms/discord/currency/bot/BotErrorHandler.java
- [X] T049 [P] 結構化日誌記錄 in src/main/resources/logback.xml（如需要）
- [X] T050 執行測試覆蓋率檢查（目標 80%）
- [X] T051 驗證所有遷移的處理器測試通過
- [X] T052 執行 quickstart.md 中的範例驗證
- [X] T053 [P] 更新架構文件 docs/architecture/overview.md（新增 discord 模組說明）
- [X] T054 [P] 更新 API 文件 docs/api/slash-commands.md（如有需要）

**Checkpoint**: ✅ Phase 7 完成 - Discord API 抽象層功能完整

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 無依賴 - 可立即開始
- **Foundational (Phase 2)**: 依賴 Setup 完成 - 阻斷所有使用者故事
- **User Stories (Phase 3-6)**: 全部依賴 Foundational 完成
  - User stories 可並行進行（如果有人力）
  - 或按優先順序序進行（P1 → P4 → P2 → P3，注意 US4 在 US2 前因為是基礎依賴）
- **Polish (Phase 7)**: 依賴所有所需使用者故事完成

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 完成後可開始 - 無其他故事依賴
- **User Story 4 (P2)**: Foundational 完成後可開始 - 獨立但與 US1 協同工作
- **User Story 2 (P2)**: Foundational 完成後可開始 - 可與 US1/US4 整合但應獨立測試
- **User Story 3 (P3)**: Foundational 完成後可開始 - 可與 US1/US2/US4 整合但應獨立測試

### Within Each User Story

- 測試必須先撰寫並失敗，然後再實作
- 介面 → JDA 實作 → Mock 實作 → Adapter → 遷移現有代碼
- 核心實作優先於整合
- 故事完成後再移至下一優先級

### Parallel Opportunities

- Setup 階段所有標記 [P] 的任務可並行
- Foundational 階段所有標記 [P] 的任務可並行（Phase 2 內）
- Foundational 完成後，所有使用者故事可並行開始（如果團隊容量允許）
- 每個故事標記 [P] 的測試可並行
- 不同使用者故事可由不同團隊成員並行工作

---

## Parallel Example: User Story 1

```bash
# 同時啟動 User Story 1 的所有測試：
Task: "DiscordInteraction 介面契約測試 in src/test/java/ltdjms/discord/discord/domain/DiscordInteractionTest.java"
Task: "JdaDiscordInteraction 實作單元測試 in src/test/java/ltdjms/discord/discord/services/JdaDiscordInteractionTest.java"
Task: "MockDiscordInteraction 實作單元測試 in src/test/java/ltdjms/discord/discord/mock/MockDiscordInteractionTest.java"
```

---

## Implementation Strategy

### MVP First (僅 User Story 1)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（關鍵 - 阻斷所有故事）
3. 完成 Phase 3: User Story 1
4. **停止並驗證**: 獨立測試 User Story 1
5. 準備好後部署/展示

### Incremental Delivery

1. 完成 Setup + Foundational → 基礎就緒
2. 加入 User Story 1 → 獨立測試 → 部署/Demo（MVP！）
3. 加入 User Story 4 → 獨立測試 → 部署/Demo
4. 加入 User Story 2 → 獨立測試 → 部署/Demo
5. 加入 User Story 3 → 獨立測試 → 部署/Demo
6. 每個故事增加價值而不破壞之前的故事

### Parallel Team Strategy

多位開發者情況：

1. 團隊一起完成 Setup + Foundational
2. Foundational 完成後：
   - 開發者 A: User Story 1 (DiscordInteraction)
   - 開發者 B: User Story 4 (DiscordContext)
   - 開發者 C: User Story 2 (DiscordEmbedBuilder)
3. 故事獨立完成並整合

---

## Notes

- [P] 任務 = 不同檔案，無依賴關係
- [Story] 標籤將任務映射到特定使用者故事以實現可追蹤性
- 每個使用者故事應獨立完成和測試
- 驗證測試在實作前失敗
- 每個任務或邏輯組後提交
- 在任何檢查點停止以獨立驗證故事
- 避免：模糊的任務、同一檔案衝突、破壞獨立性的跨故事依賴

---

## Summary

- **Total Task Count**: 54 tasks
- **Completed**: 54/54 tasks (100%)
- **Task Count per User Story**:
  - ✅ Setup (Phase 1): 3/3 tasks 完成
  - ✅ Foundational (Phase 2): 4/4 tasks 完成
  - ✅ User Story 1 (P1): 9/9 tasks 完成 (3 tests + 6 implementation)
  - ✅ User Story 4 (P2): 8/8 tasks 完成 (3 tests + 5 implementation)
  - ✅ User Story 2 (P2): 8/8 tasks 完成 (3 tests + 5 implementation)
  - ✅ User Story 3 (P3): 11/11 tasks 完成 (3 tests + 8 implementation)
  - ✅ Polish (Phase 7): 10/10 tasks 完成

- **Current Status**: ✅ **所有任務已完成** - Discord API 抽象層功能完整且可投入使用

- **Parallel Opportunities**:
  - Setup: 2 parallel tasks
  - Foundational: 4 parallel tasks
  - Each User Story: 3 parallel tests initially
  - Polish: 6 parallel tasks

- **Independent Test Criteria**:
  - US1: ✅ 使用抽象介面的餘額查詢命令
  - US4: ✅ 使用 DiscordContext 提取參數
  - US2: ✅ 使用 DiscordEmbedBuilder 建構 Embed
  - US3: ✅ 使用 DiscordSessionManager 管理面板

- **Completed Features**:
  - User Story 1: DiscordInteraction 介面與 JDA/Mock 實作
  - User Story 4: DiscordContext 介面與 JDA/Mock 實作
  - User Story 2: DiscordEmbedBuilder 介面與長度限制處理
  - User Story 3: DiscordSessionManager 泛型介面與 Session 管理
  - Polish: DiscordButtonEvent/DiscordModalEvent 介面、ModalInteractionAdapter、BotErrorHandler 更新、日誌配置、文件更新

- **Remaining Work**: 無 - 所有任務已完成

- **Format Validation**: ✅ ALL tasks follow the checklist format (checkbox, ID, labels, file paths)
