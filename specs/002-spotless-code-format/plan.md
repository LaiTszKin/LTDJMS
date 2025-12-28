# Implementation Plan: Spotless Code Format

**Branch**: `001-spotless-code-format` | **Date**: 2025-12-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-spotless-code-format/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

引入 Spotless Maven 外掛來自動化 Java 代碼格式檢查和格式化，確保代碼庫遵循統一的編碼風格（基於 Google Java Format）。這包括配置 Maven 外掛、執行一次性格式化調整現有代碼、整合格式檢查到建置流程、以及提供 IDE 配置支援。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spotless Maven Plugin (需決定版本), Google Java Format
**Storage**: N/A (此功能不涉及資料庫變更)
**Testing**: JUnit 5.11.3 (現有), Mockito 5.14.2 (現有)
**Target Platform**: JVM (現有 Discord 機器人環境)
**Project Type**: Single Maven project (現有結構)
**Performance Goals**: 格式檢查 < 30 秒執行時間; 格式化操作不影響代碼邏輯
**Constraints**: 必須排除 Dagger 生成的 DI 組件; 不應破壞現有測試
**Scale/Scope**: 約 ~50+ Java 源代碼檔案需要格式化

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on `.specify/memory/constitution.md` v1.0.0:

- [N/A] **I. Test-Driven Development**: Feature MUST start with failing tests, achieve 80% coverage
  - **RATIONALE**: 此功能為建置工具配置（Spotless Maven Plugin），不涉及新的業務邏輯代碼。代碼格式化工具本身由 Spotless 團隊維護並已充分測試。現有代碼的測試覆蓋率將透過現有的 JaCoCo 配置驗證（SC-002：格式化後所有測試仍通過）。
- [N/A] **II. Domain-Driven Design**: Feature MUST respect layered architecture (domain/persistence/services/commands)
  - **RATIONALE**: 此功能不引入新的業務功能，僅配置 Maven 外掛進行代碼格式化。不涉及新的 domain、persistence、services 或 commands 層組件。
- [N/A] **III. Configuration Flexibility**: All new config MUST be externalizable (env/.env/conf)
  - **RATIONALE**: Spotless 配置在 `pom.xml` 中作為建置配置，這是 Maven 標準做法。格式規則（Google Java Format）是編碼標準，不屬於運行時環境配置。
- [N/A] **IV. Database Schema Management**: Schema changes MUST use Flyway migrations
  - **RATIONALE**: 此功能不涉及資料庫變更。
- [N/A] **V. Observability**: New operations MUST include structured logging and metrics
  - **RATIONALE**: 代碼格式化是建置時工具，非運行時操作。Maven 外掛會提供格式違規報告作為輸出。
- [N/A] **VI. Dependency Injection**: All new components MUST use Dagger 2 injection
  - **RATIONALE**: 此功能不引入新的運行時組件。
- [N/A] **VII. Error Handling**: All errors MUST use `Result<T, DomainError>` pattern with user-friendly Discord messages
  - **RATIONALE**: 格式檢查在建置時執行，失敗會導致建置中止並顯示格式違規報告（Maven 標準輸出），非 Discord 互動。

**Development Standards Compliance**:
- [x] Code uses Java 17+ features appropriately - 格式化工具將確保 Java 17 代碼風格一致性
- [N/A] Public APIs include Javadoc - 此功能不引入新的公開 API
- [x] Documentation updates planned - 將更新 `docs/development/testing.md` 包含格式檢查指令
- [x] Follows Conventional Commits format - 提交將遵循 `feat(build)` 或 `chore` 格式

**GATE STATUS**: PASS - 所有 N/A 項目均已提供合理理由，此功能屬於建置工具配置，非業務功能開發。

## Project Structure

### Documentation (this feature)

```text
specs/001-spotless-code-format/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # N/A - 此功能不涉及資料模型
├── quickstart.md        # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

此功能修改現有專案結構，不引入新的目錄：

```text
pom.xml                    # 修改：新增 Spotless Maven Plugin 配置
src/main/java/             # 現有：所有 Java 源代碼將被格式化
docs/development/testing.md # 修改：新增格式檢查指令文檔
```

**Structure Decision**: Single Maven project（現有結構）。此功能在現有單一 Maven 專案上新增 Spotless 外掛配置，不引入新的 Java 類別或模組。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

此功能的 Constitution Check 全部為 N/A（建置工具配置類），無違規需要證明。
