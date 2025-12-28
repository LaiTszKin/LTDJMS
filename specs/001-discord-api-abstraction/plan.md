# Implementation Plan: Discord API 抽象層

**Branch**: `001-discord-api-abstraction` | **Date**: 2025-12-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-discord-api-abstraction/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

此功能旨在將現有業務邏輯中對 Discord API（JDA）的直接調用抽象成一個獨立可擴展的 API 模組。通過建立 `DiscordInteraction`、`DiscordEmbedBuilder`、`DiscordSessionManager`、`DiscordContext` 等抽象介面，並使用 Adapter 模式將 JDA 實作適配到這些介面，實現業務邏輯與 Discord API 實作的解耦。技術方法將在研究階段確定。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: JDA 5.2.2 (現有), Dagger 2.52, JUnit 5.11.3, Mockito 5.14.2
**Storage**: N/A (此功能不涉及資料庫變更)
**Testing**: JUnit 5 + Mockito + AssertJ (現有)
**Target Platform**: Discord bot (Java 應用程式)
**Project Type**: single
**Performance Goals**: 抽象層效能開銷低於 5% (SC-007)
**Constraints**:
- 向後相容性：現有功能遷移後無破壞性變更 (FR-011, SC-001)
- Session 記憶體使用量：低於 10MB (1000 個活躍 Session) (SC-008)
- 測試覆蓋率：最低 80% (QR-002)
**Scale/Scope**:
- 需遷移的命令處理器：currency/, gametoken/, panel/, shop/, redemption/ 模組
- 抽象介面數量：約 4-5 個核心介面
- 視圖元件建構器：Embed、Button、SelectMenu、Modal

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on `.specify/memory/constitution.md` v1.0.0:

- [x] **I. Test-Driven Development**: Feature MUST start with failing tests, achieve 80% coverage
  - **Status**: COMPLIANT - Design includes Mock implementations (`MockDiscordInteraction`, etc.) for unit testing
  - **Verification**: 浬試策略定義於 research.md 第 5 節，包括 70% 單元測試 + 20% 整合測試 + 10% E2E 測試

- [x] **II. Domain-Driven Design**: Feature MUST respect layered architecture (domain/persistence/services/commands)
  - **Status**: COMPLIANT - New `discord/` module follows pattern: `domain/` (interfaces) → `services/` (JDA impl) → `adapter/` (event adapters)
  - **Verification**: data-model.md 定義了清晰的實體關聯圖和介面契約

- [x] **III. Configuration Flexibility**: All new config MUST be externalizable (env/.env/conf)
  - **Status**: COMPLIANT - No new configuration required (TTL 使用硬編碼常數，符合 SC-008)

- [x] **IV. Database Schema Management**: Schema changes MUST use Flyway migrations
  - **Status**: N/A - This feature does not involve database changes (explicitly stated in QR-006)

- [x] **V. Observability**: New operations MUST include structured logging and metrics
  - **Status**: COMPLIANT - contracts/discord-api-contracts.md 定義了 `DiscordError` 類別，擴展現有日誌基礎設施
  - **Verification**: research.md 第 4 節定義了統一錯誤處理策略

- [x] **VI. Dependency Injection**: All new components MUST use Dagger 2 injection
  - **Status**: COMPLIANT - quickstart.md 包含 `DiscordModule` Dagger 模組定義
  - **Verification**: 所有抽象實作將透過 `@Inject` 建構函式注入

- [x] **VII. Error Handling**: All errors MUST use `Result<T, DomainError>` pattern with user-friendly Discord messages
  - **Status**: COMPLIANT - contracts/discord-api-contracts.md 定義了 `DiscordError` 與現有 `DomainError` 的整合
  - **Verification**: research.md 第 4 節定義了錯誤處理策略和 Ephemeral 回應

**Development Standards Compliance**:
- [x] Code uses Java 17+ features (records used for value objects: `EmbedView`, `ButtonView`, `Session`)
- [x] Public APIs include Javadoc (contracts/discord-api-contracts.md 包含完整介面文件)
- [x] Documentation updates planned (quickstart.md 提供遷移指南和使用範例)
- [x] Follows Conventional Commits format

**Post-Design Re-evaluation**:
- 所有抽象介面使用 Java 17 語言特性（介面方法、records）
- 測試策略不依賴 JDA Mock（使用 Mock 實作）
- 錯誤處理與現有 `Result<T, DomainError>` 模式完全整合
- Session 管理使用泛型設計，符合 DDD 原則

**Overall Gate Status**: **PASS** - All constitution requirements satisfied. Design phase confirmed compliance.

## Project Structure

### Documentation (this feature)

```text
specs/001-discord-api-abstraction/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/ltdjms/discord/
├── discord/                    # NEW: Discord API abstraction layer
│   ├── domain/                 # Abstract interfaces
│   │   ├── DiscordInteraction.java
│   │   ├── DiscordEmbedBuilder.java
│   │   ├── DiscordSessionManager.java
│   │   ├── DiscordContext.java
│   │   ├── EmbedView.java
│   │   ├── ButtonView.java
│   │   ├── SelectMenuView.java
│   │   ├── ModalView.java
│   │   └── Session.java
│   ├── services/               # Abstract implementations (JDA-based)
│   │   ├── JdaDiscordInteraction.java
│   │   ├── JdaDiscordEmbedBuilder.java
│   │   ├── JdaDiscordSessionManager.java
│   │   └── JdaDiscordContext.java
│   ├── mock/                   # Mock implementations for testing
│   │   ├── MockDiscordInteraction.java
│   │   ├── MockDiscordEmbedBuilder.java
│   │   ├── MockDiscordSessionManager.java
│   │   └── MockDiscordContext.java
│   └── adapter/                # JDA adapters
│       ├── SlashCommandAdapter.java
│       ├── ButtonInteractionAdapter.java
│       └── ModalInteractionAdapter.java
├── currency/                   # EXISTING: Gradual migration to abstraction
│   ├── commands/
│   │   └── BalanceAdjustmentCommandHandler.java  # Migrate to use DiscordInteraction
│   └── ...
├── panel/                      # EXISTING: Migrate to use DiscordSessionManager
│   ├── commands/
│   │   └── UserPanelButtonHandler.java  # Migrate to use DiscordInteraction
│   └── services/
│       └── UserPanelEmbedBuilder.java  # Migrate to use DiscordEmbedBuilder
├── shared/                     # EXISTING: Result, DomainError, etc.
│   └── ...
└── shared/di/                  # EXISTING: Add DiscordModule
    └── DiscordModule.java      # NEW: Register abstract implementations
```

```text
src/test/java/ltdjms/discord/discord/
├── domain/                     # Interface contracts
│   ├── DiscordInteractionTest.java
│   ├── DiscordEmbedBuilderTest.java
│   ├── DiscordSessionManagerTest.java
│   └── DiscordContextTest.java
├── services/                   # JDA implementation tests
│   ├── JdaDiscordInteractionTest.java
│   ├── JdaDiscordEmbedBuilderTest.java
│   ├── JdaDiscordSessionManagerTest.java
│   └── JdaDiscordContextTest.java
├── mock/                       # Mock implementation tests
│   ├── MockDiscordInteractionTest.java
│   ├── MockDiscordEmbedBuilderTest.java
│   ├── MockDiscordSessionManagerTest.java
│   └── MockDiscordContextTest.java
└── adapter/                    # Adapter tests
    ├── SlashCommandAdapterTest.java
    ├── ButtonInteractionAdapterTest.java
    └── ModalInteractionAdapterTest.java
```

**Structure Decision**: 選擇單一專案結構（Option 1）。新增 `discord/` 模組作為共享基礎設施，包含抽象介面定義、JDA 實作、Mock 實作和 Adapter。現有模組（currency/, panel/, shop/ 等）將逐步遷移到使用抽象介面。這種結構確保：
1. 清晰的模組邊界（`discord/domain/` 定義契約，`discord/services/` 提供 JDA 實作）
2. 現有模組可獨立遷移，無需大規模重構
3. 測試代碼與生產代碼分離，Mock 實作用於單元測試

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A - No constitution violations |

**Note**: No violations detected. All requirements align with existing constitution principles.
