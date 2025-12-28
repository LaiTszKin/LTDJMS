# Implementation Plan: AI Chat Mentions

**Branch**: `003-ai-chat` | **Date**: 2025-12-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-ai-chat/spec.md`

## Summary

實作一個 AI 聊天功能，當使用者在 Discord 頻道中提及機器人時，機器人會使用 AI 服務生成並發送回應訊息。系統不保存對話歷史，管理員可透過 .env 檔案配置 AI 服務參數（base URL、API 金鑰、模型名稱等）。

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: JDA 5.2.2 (Discord API), Dagger 2.52 (DI), SLF4J 2.0.16 + Logback 1.5.12 (Logging)
**HTTP Client**: Java 17 內建 HttpClient (java.net.http)
**JSON Library**: Jackson 2.17.2 (jackson-databind)
**Storage**: N/A (此功能不涉及資料庫持久化)
**Testing**: JUnit 5.11.3, Mockito 5.14.2, AssertJ 3.26.3, Testcontainers 1.20.4
**Target Platform**: Java 17+ 應用伺服器
**Project Type**: 單一專案 - 模組化架構
**Performance Goals**:
  - 5 秒內回應 AI 請求 (SC-001)
  - 支援 100 個並行請求 (SC-002)
  - 95% AI 服務呼叫成功率 (SC-003)
**Constraints**:
  - Discord 訊息長度限制 2000 字元 (需分割長回應)
  - Discord 互動逾時 15 秒
  - AI 服務逾時 30 秒 (可配置)
**Scale/Scope**:
  - 單一新模組 `aichat`
  - 無狀態設計 (不保存對話歷史)
  - 支援所有 Discord 頻道

## Phase 0: Research (Completed)

所有技術決策已完成，詳見 [research.md](./research.md)：

- ✅ HTTP Client: Java 17 內建 HttpClient
- ✅ JSON 序列化: Jackson 2.17.2
- ✅ AI 服務: OpenAI Chat Completions API 標準
- ✅ 訊息分割: 智慧分割（保留段落完整性）
- ✅ 錯誤處理: 擴展 DomainError
- ✅ 配置管理: .env + EnvironmentConfig
- ✅ 事件監聽: JDA GenericEventMonitor
- ✅ 日誌記錄: 結構化日誌 + MDC

## Phase 1: Design (Completed)

所有設計產物已生成：

- ✅ [data-model.md](./data-model.md) - 領域模型定義
- ✅ [contracts/openapi.yaml](./contracts/openapi.yaml) - AI 服務 API 契約
- ✅ [quickstart.md](./quickstart.md) - 快速入門指南

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on `.specify/memory/constitution.md` v1.0.0:

- [ ] **I. Test-Driven Development**: Feature MUST start with failing tests, achieve 80% coverage
- [ ] **II. Domain-Driven Design**: Feature MUST respect layered architecture (domain/persistence/services/commands)
- [x] **III. Configuration Flexibility**: All new config MUST be externalizable (env/.env/conf) - 設計符合
- [x] **IV. Database Schema Management**: Schema changes MUST use Flyway migrations - N/A (此功能不涉及資料庫)
- [ ] **V. Observability**: New operations MUST include structured logging and metrics
- [ ] **VI. Dependency Injection**: All new components MUST use Dagger 2 injection
- [ ] **VII. Error Handling**: All errors MUST use `Result<T, DomainError>` pattern with user-friendly Discord messages

**Development Standards Compliance**:
- [ ] Code uses Java 17+ features appropriately
- [ ] Public APIs include Javadoc
- [ ] Documentation updates planned (docs/modules/, docs/api/)
- [ ] Follows Conventional Commits format

## Project Structure

### Documentation (this feature)

```text
specs/003-ai-chat/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── openapi.yaml     # AI Service API contract
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/java/ltdjms/discord/aichat/
├── domain/
│   ├── AIChatRequest.java          # AI 聊天請求模型
│   ├── AIChatResponse.java         # AI 回應模型
│   ├── AIServiceConfig.java        # AI 服務配置
│   └── AIMessageEvent.java         # AI 訊息事件
├── persistence/                    # (不適用 - 無資料庫持久化)
├── services/
│   ├── AIChatService.java          # AI 聊天服務介面
│   ├── DefaultAIChatService.java   # 預設實作
│   └── AIClient.java               # AI HTTP 客戶端
└── commands/
    └── AIChatMentionListener.java  # 提及事件監聽器

src/test/java/ltdjms/discord/aichat/
├── unit/
│   ├── AIChatServiceTest.java
│   └── AIClientTest.java
└── integration/
    └── AIChatIntegrationTest.java
```

**Structure Decision**: 遵循現有的 DDD 分層架構模式。新模組 `aichat` 將包含：
- **domain/**: 純業務模型 (請求、回應、配置)
- **services/**: AI HTTP 客戶端與聊天服務實作
- **commands/**: JDA 事件監聽器
- **persistence/**: 不適用 (此功能不需要資料庫持久化)

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | 無違規項目 | 此設計完全符合憲法要求 |

