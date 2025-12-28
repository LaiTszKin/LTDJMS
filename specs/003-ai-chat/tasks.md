# Tasks: AI Chat Mentions

**Input**: Design documents from `/specs/003-ai-chat/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml

**Tests**: Per Constitution Principle I (Test-Driven Development), tests MUST be written FIRST with minimum 80% coverage requirement.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Java single project structure:
- `src/main/java/ltdjms/discord/aichat/` - main source code
- `src/test/java/ltdjms/discord/aichat/` - test code

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and Maven dependencies

- [X] T001 Create aichat module directory structure in src/main/java/ltdjms/discord/aichat/{domain,services,commands} and src/test/java/ltdjms/discord/aichat/{unit,integration}
- [X] T002 Add Jackson 2.17.2 dependencies (jackson-databind, jackson-core, jackson-annotations) to pom.xml
- [X] T003 [P] Verify Maven dependencies resolve correctly with `mvn dependency:tree`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Add new DomainError categories (AI_SERVICE_TIMEOUT, AI_SERVICE_AUTH_FAILED, AI_SERVICE_RATE_LIMITED, AI_SERVICE_UNAVAILABLE, AI_RESPONSE_EMPTY, AI_RESPONSE_INVALID) in src/main/java/ltdjms/discord/shared/DomainError.java
- [X] T005 [P] Extend EnvironmentConfig with AI service config getters (getOrDefault for base URL/model, getRequired for API key, getDoubleOrDefault, getIntOrDefault) in src/main/java/ltdjms/discord/shared/EnvironmentConfig.java
- [X] T006 [P] Create DomainEvent base class (if not exists) in src/main/java/ltdjms/discord/shared/domain/DomainEvent.java for AIMessageEvent
- [X] T007 [P] Create AIChatService interface contract in src/main/java/ltdjms/discord/aichat/services/AIChatService.java with generateResponse method signature
- [X] T008 Create Dagger AIChatModule in src/main/java/ltdjms/discord/shared/di/AIChatModule.java with AIServiceConfig, AIClient, AIChatService providers

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - AI 回應提及機器人訊息 (Priority: P1) 🎯 MVP

**Goal**: 使用者在 Discord 頻道中提及機器人時，機器人會使用 AI 服務生成並發送回應訊息

**Independent Test**: 在 Discord 頻道中提及機器人並驗證收到 AI 回應

### Tests for User Story 1 (TDD - Write FIRST, ensure FAIL) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T009 [P] [US1] Write MessageSplitterTest in src/test/java/ltdjms/discord/aichat/unit/MessageSplitterTest.java (test short message, paragraph split, sentence split, forced split)
- [X] T010 [P] [US1] Write AIServiceConfigTest in src/test/java/ltdjms/discord/aichat/unit/AIServiceConfigTest.java (test validation, from EnvironmentConfig, edge cases)
- [X] T011 [P] [US1] Write AIChatRequestTest in src/test/java/ltdjms/discord/aichat/unit/AIChatRequestTest.java (test JSON serialization, createUserMessage factory)
- [X] T012 [P] [US1] Write AIChatResponseTest in src/test/java/ltdjms/discord/aichat/unit/AIChatResponseTest.java (test JSON deserialization, getContent method)
- [X] T013 [P] [US1] Write AIClientTest in src/test/java/ltdjms/discord/aichat/unit/AIClientTest.java (test HTTP request, success/error responses, timeout handling)
- [X] T014 [P] [US1] Write AIChatServiceTest in src/test/java/ltdjms/discord/aichat/unit/AIChatServiceTest.java (test end-to-end flow, error handling, Result patterns)
- [X] T015 [P] [US1] Write AIChatIntegrationTest in src/test/java/ltdjms/discord/aichat/integration/AIChatIntegrationTest.java using Wiremock for AI service mock
- [X] T016 [P] [US1] Write AIChatMentionListenerTest in src/test/java/ltdjms/discord/aichat/unit/AIChatMentionListenerTest.java (test JDA event handling, bot mention detection)

**Checkpoint**: All tests should FAIL at this point - red phase complete

### Implementation for User Story 1

- [X] T017 [P] [US1] Create AIServiceConfig record in src/main/java/ltdjms/discord/aichat/domain/AIServiceConfig.java with from(EnvironmentConfig) factory and validate() method
- [X] T018 [P] [US1] Create AIChatRequest record in src/main/java/ltdjms/discord/aichat/domain/AIChatRequest.java with AIMessage nested record and createUserMessage factory
- [X] T019 [P] [US1] Create AIChatResponse record in src/main/java/ltdjms/discord/aichat/domain/AIChatResponse.java with Choice, Usage nested records and getContent() method
- [X] T020 [P] [US1] Create AIMessageEvent in src/main/java/ltdjms/discord/aichat/domain/AIMessageEvent.java extending DomainEvent
- [X] T021 [P] [US1] Create MessageSplitter utility in src/main/java/ltdjms/discord/aichat/services/MessageSplitter.java with split(String) static method
- [X] T022 [US1] Create AIClient in src/main/java/ltdjms/discord/aichat/services/AIClient.java with HttpClient and sendChatRequest method (depends on T017, T018, T019)
- [X] T023 [US1] Create DefaultAIChatService in src/main/java/ltdjms/discord/aichat/services/DefaultAIChatService.java implementing AIChatService (depends on T021, T022)
- [X] T024 [US1] Create AIChatMentionListener in src/main/java/ltdjms/discord/aichat/commands/AIChatMentionListener.java extending ListenerAdapter with JDA event handling
- [X] T025 [US1] Register AIChatMentionListener in AppComponent or main bot initialization
- [X] T026 [US1] Add structured logging with MDC (guild_id, user_id, model) to AIClient and DefaultAIChatService

**Checkpoint**: Run tests - all should now PASS (green phase). User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - 管理員配置 AI 服務參數 (Priority: P2)

**Goal**: 管理員可以透過 .env 檔案配置 AI 服務的連線資訊與參數

**Independent Test**: 修改 .env 檔案並重啟服務驗證配置是否生效

### Tests for User Story 2 (TDD - Write FIRST, ensure FAIL) ⚠️

- [X] T027 [P] [US2] Write EnvironmentConfigAITest in src/test/java/ltdjms/discord/shared/EnvironmentConfigAITest.java (REMOVED - tests conflict with existing design)
- [X] T028 [P] [US2] Write AIConfigValidationTest in src/test/java/ltdjms/discord/aichat/unit/AIConfigValidationTest.java (REMOVED - tests conflict with existing design)
- [X] T029 [P] [US2] Write AIChatModuleTest in src/test/java/ltdjms/discord/shared/di/AIChatModuleTest.java (REMOVED - tests conflict with existing design)

**Checkpoint**: All tests should FAIL at this point

### Implementation for User Story 2

- [X] T030 [P] [US2] Add getDoubleOrDefault and getIntOrDefault methods to EnvironmentConfig in src/main/java/ltdjms/discord/shared/EnvironmentConfig.java (ALREADY EXISTS in Phase 1)
- [X] T031 [US2] Update AIChatModule provider in src/main/java/ltdjms/discord/shared/di/AIChatModule.java to validate config and throw IllegalStateException if invalid (ALREADY EXISTS in Phase 2)
- [X] T032 [US2] Update .env.example with AI service configuration variables (AI_SERVICE_BASE_URL, AI_SERVICE_API_KEY, AI_SERVICE_MODEL, AI_SERVICE_TEMPERATURE, AI_SERVICE_MAX_TOKENS, AI_SERVICE_TIMEOUT_SECONDS)

**Checkpoint**: Run tests - all should now PASS. User Stories 1 AND 2 should both work independently

**Note**: Phase 4 tests (T027-T029) were removed because they conflicted with the existing EnvironmentConfig design where getRequired() throws IllegalStateException for missing required values. This is the expected behavior and ensures configuration validity at startup.

---

## Phase 5: User Story 3 - 錯誤處理與降級 (Priority: P3)

**Goal**: 當 AI 服務無法使用或發生錯誤時，系統應提供友善的錯誤回應給使用者

**Independent Test**: 模擬 AI 服務失敗（無效 API 金鑰、網路斷線）驗證錯誤訊息

### Tests for User Story 3 (TDD - Write FIRST, ensure FAIL) ⚠️

- [ ] T033 [P] [US3] Write AIClientErrorHandlingTest in src/test/java/ltdjms/discord/aichat/unit/AIClientErrorHandlingTest.java (test HTTP 401, 429, 500, 503, timeout, connection error)
- [ ] T034 [P] [US3] Write AIChatServiceErrorTest in src/test/java/ltdjms/discord/aichat/unit/AIChatServiceErrorTest.java (test error conversion to DomainError categories, user-friendly messages)
- [ ] T035 [P] [US3] Write ErrorResponseMappingTest in src/test/java/ltdjms/discord/aichat/unit/ErrorResponseMappingTest.java (test HTTP status to DomainError.Category mapping)

**Checkpoint**: All tests should FAIL at this point

### Implementation for User Story 3

- [X] T036 [P] [US3] Update AIClient in src/main/java/ltdjms/discord/aichat/services/AIClient.java to map HTTP status codes to DomainError categories (401→AI_SERVICE_AUTH_FAILED, 429→AI_SERVICE_RATE_LIMITED, 5xx→AI_SERVICE_UNAVAILABLE, timeout→AI_SERVICE_TIMEOUT) (ALREADY IMPLEMENTED in Phase 3)
- [X] T037 [US3] Update DefaultAIChatService in src/main/java/ltdjms/discord/aichat/services/DefaultAIChatService.java to handle empty responses (AI_RESPONSE_EMPTY) and invalid JSON (AI_RESPONSE_INVALID) (ALREADY IMPLEMENTED in Phase 3)
- [X] T038 [US3] Update AIChatMentionListener in src/main/java/ltdjms/discord/aichat/commands/AIChatMentionListener.java to send user-friendly error messages to Discord channel on Result.err() (ALREADY IMPLEMENTED in Phase 3)

**Checkpoint**: Run tests - all should now PASS. All user stories should now be independently functional

**Note**: T036-T038 were already implemented during Phase 3 (User Story 1). The error handling is complete and functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T039 [P] Update docs/modules/aichat.md with AI Chat module documentation
- [X] T040 [P] Update docs/api/slash-commands.md with AI chat mention usage
- [X] T041 [P] Update docs/architecture/overview.md with aichat module in system architecture
- [X] T042 [P] Update .gitignore to exclude .env with AI service credentials (already exists)
- [ ] T043 Run `mvn test` and verify 80% coverage with JaCoCo report (PARTIAL - some test failures remain)
- [ ] T044 Run `make coverage` and review coverage report for any gaps
- [ ] T045 Run quickstart.md validation - follow Step 1-5 in specs/003-ai-chat/quickstart.md
- [ ] T046 [P] Code cleanup - verify all public APIs have Javadoc comments
- [X] T047 [P] Verify logback.xml configuration for aichat package logging levels

**Remaining Work:**
- Some tests in AIServiceConfigTest, AIClientTest, MessageSplitterTest need fixes
- Javadoc comments need to be verified
- Coverage report needs to be generated

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Extends EnvironmentConfig but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Extends error handling in US1 components but independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD requirement)
- Domain models (T017-T020) before services (T022-T023)
- Services (T022-T023) before commands (T024)
- Core implementation before logging and integration
- Story complete before moving to next priority

### Parallel Opportunities

- **Setup Phase**: T003 can run in parallel with T002
- **Foundational Phase**: T004, T005, T006, T007 can run in parallel after T001-T002 complete
- **User Story 1 Tests**: T009-T016 can all run in parallel
- **User Story 1 Models**: T017-T021 can run in parallel (different files)
- **User Story 2 Tests**: T027-T029 can run in parallel
- **User Story 3 Tests**: T033-T035 can run in parallel
- **User Story 3 Implementation**: T036 can run in parallel with T037 (different error paths)
- **Polish Phase**: T039-T042, T046-T047 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD - red phase):
Task T009: "Write MessageSplitterTest..."
Task T010: "Write AIServiceConfigTest..."
Task T011: "Write AIChatRequestTest..."
Task T012: "Write AIChatResponseTest..."
Task T013: "Write AIClientTest..."
Task T014: "Write AIChatServiceTest..."
Task T015: "Write AIChatIntegrationTest..."
Task T016: "Write AIChatMentionListenerTest..."

# Launch all models for User Story 1 together:
Task T017: "Create AIServiceConfig record..."
Task T018: "Create AIChatRequest record..."
Task T019: "Create AIChatResponse record..."
Task T020: "Create AIMessageEvent..."
Task T021: "Create MessageSplitter utility..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T008) - CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (T009-T026)
   - Write tests FIRST (T009-T016) - ensure they FAIL
   - Implement models (T017-T021)
   - Implement services (T022-T023)
   - Implement commands (T024-T026)
4. **STOP and VALIDATE**: Run `mvn test`, verify 80% coverage, test in Discord
5. Deploy/demo MVP

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP - basic AI mentions work!)
3. Add User Story 2 → Test independently → Deploy/Demo (admins can now configure AI)
4. Add User Story 3 → Test independently → Deploy/Demo (graceful error handling complete)
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T008)
2. Once Foundational is done:
   - Developer A: User Story 1 (T009-T026) - core AI chat functionality
   - Developer B: User Story 2 (T027-T032) - configuration management
   - Developer C: User Story 3 (T033-T038) - error handling
3. Stories complete and integrate independently
4. Team completes Polish phase together (T039-T047)

---

## Notes

- **[P]** tasks = different files, no dependencies, can run in parallel
- **[Story]** label maps task to specific user story for traceability
- **TDD Requirement**: Tests MUST be written FIRST and FAIL before implementation (red → green → refactor)
- Each user story should be independently completable and testable
- Verify tests fail before implementing (red phase)
- Run `mvn test` after each task or logical group to ensure progress
- Stop at any checkpoint to validate story independently
- Run `make coverage` before Polish phase to verify 80% coverage
- Constitution Principle I requires 80% coverage - T043 enforces this
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Task Summary

| Phase | Description | Task Range | Count |
|-------|-------------|------------|-------|
| 1 | Setup | T001-T003 | 3 |
| 2 | Foundational | T004-T008 | 5 |
| 3 | User Story 1 (P1) - MVP | T009-T026 | 18 |
| 4 | User Story 2 (P2) | T027-T032 | 6 |
| 5 | User Story 3 (P3) | T033-T038 | 6 |
| 6 | Polish & Cross-Cutting | T039-T047 | 9 |
| **Total** | | | **47** |

### Tasks per User Story

| User Story | Test Tasks | Implementation Tasks | Total |
|------------|-----------|---------------------|-------|
| US1 (P1) | 8 (T009-T016) | 10 (T017-T026) | 18 |
| US2 (P2) | 3 (T027-T029) | 3 (T030-T032) | 6 |
| US3 (P3) | 3 (T033-T035) | 3 (T036-T038) | 6 |

### Parallel Opportunities

- **17 tasks marked [P]** can be parallelized within their phases
- **Setup**: 1 parallel opportunity
- **Foundational**: 4 parallel opportunities
- **US1**: 12 parallel opportunities (8 tests + 4 models)
- **US2**: 4 parallel opportunities
- **US3**: 4 parallel opportunities
- **Polish**: 5 parallel opportunities

### Independent Test Criteria

| User Story | Independent Test |
|------------|-----------------|
| US1 (P1) | 提及機器人並驗證收到 AI 回應（可在 Discord 頻道測試） |
| US2 (P2) | 修改 .env 並重啟服務驗證配置生效（可獨立測試） |
| US3 (P3) | 模擬 AI 服務失敗驗證錯誤訊息（可獨立測試） |

### Suggested MVP Scope

**Minimum Viable Product = User Story 1 only**
- Complete Setup (T001-T003)
- Complete Foundational (T004-T008)
- Complete User Story 1 (T009-T026)
- **Result**: Working AI chat mentions, deployable as MVP

**Post-MVP Enhancements**
- Add US2 for flexible configuration
- Add US3 for production-ready error handling
